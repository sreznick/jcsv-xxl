package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.Utility;
import ru.study21.jcsv.xxl.io.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class ExternalSorter {

    // use the entire pipeline (for the largest files)
    public static void sort_maximum(
            Path inputCsvFile,
            CSVReader inputCsvReader,
            SortDescription sortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams, // only for key columns (!)
            Path outputCsvFile,
            long approxMemoryLimit,
            int writingCacheSize, // also used as reading cache size when necessary
            int raCacheCount
    ) throws IOException, BrokenContentsException {
        FileManager fm = FileManager.createTempDirectory("sort");
        Path indexBinaryFile = fm.createTempFile("binaryIndex");
        Path sortedIndexBinaryFile = fm.createTempFile("sortedBinaryIndex");
        Path offsetsFile = fm.createTempFile("offsets");

        // map necessary columns to new indices in the index file
        List<SortDescription.KeyElement> indexSortDescElements = new ArrayList<>(sortDesc.keys().size() + 1);
        List<Integer> toCut = new ArrayList<>(sortDesc.keys().size());
        for (int i = 0; i < sortDesc.keys().size(); i++) {
            SortDescription.KeyElement element = sortDesc.keys().get(i);
            indexSortDescElements.add(new SortDescription.KeyElement(i, element.keyType(), element.order()));
            toCut.add(element.field());
        }
        indexSortDescElements.add(SortDescription.KeyElement.asLong(indexSortDescElements.size())); // enumeration column
        SortDescription indexSortDesc = SortDescription.of(indexSortDescElements);
        List<CSVFileBinarizer.ColumnTypeBinarizationParams> indexBinParams = Utility.slice(binParams, toCut);
        indexBinParams.add(new CSVFileBinarizer.LongCTBS()); // enumeration

        // inputReader -> cut -> enumerate = create index, add enumeration param
        CuttingCSVReader cutter = new CuttingCSVReader(inputCsvReader, toCut);
        EnumeratorCSVReader enumerator = new EnumeratorCSVReader(cutter);

        // !!!
        // stages that consume memory: batchSort, merge, permute
        // they should be separated to different functions for GC to work
        // !!!

        int entryLen = CSVFileBinarizer.calcSumByteLength(indexBinParams);
        long indexBatchSize = approxMemoryLimit / entryLen;

        // batchSort input and binarize
        doBatchSortAndBinarize(indexBinaryFile, indexSortDesc, indexBinParams, enumerator, indexBatchSize);

        // merge index
        doTwoPassMerge(approxMemoryLimit, writingCacheSize, indexBinaryFile, sortedIndexBinaryFile, indexSortDesc, indexBinParams, entryLen, indexBatchSize);

        // permute source = generate offsets + for (pos in index) { read offsets and write offset value to output }
        // in fact, no need to debinarize index
        try (
                FileChannel inputChannel = FileChannel.open(inputCsvFile, StandardOpenOption.READ);
                FileChannel offsetsChannel = FileChannel.open(offsetsFile, StandardOpenOption.WRITE)
        ) {
            // offsets
            CSVOffsetExtractor.extractOffsets(inputChannel, offsetsChannel, writingCacheSize, writingCacheSize);
        }
        doPermuteAndWriteOutput(inputCsvFile, outputCsvFile, approxMemoryLimit, writingCacheSize, raCacheCount, sortedIndexBinaryFile, offsetsFile, entryLen);
    }

    private static void doPermuteAndWriteOutput(
            Path inputCsvFile,
            Path outputCsvFile,
            long approxMemoryLimit,
            int writingCacheSize,
            int raCacheCount,
            Path sortedIndexBinaryFile,
            Path offsetsFile,
            int entryLen
    ) throws IOException {
        try (
                FileChannel inputChannel = FileChannel.open(inputCsvFile, StandardOpenOption.READ);
                FileChannel offsetsChannel = FileChannel.open(offsetsFile, StandardOpenOption.READ);
                ByteChannel sortedIndexByteChannel = Files.newByteChannel(sortedIndexBinaryFile, StandardOpenOption.READ);
                ByteChannel outputByteChannel = Files.newByteChannel(outputCsvFile, StandardOpenOption.WRITE);
                CachedNioBinaryWriter outputWriter = new CachedNioBinaryWriter(outputByteChannel, writingCacheSize)
        ) {
            // permuting
            RandomAccessCachedNioReader raSourceReader = new RandomAccessCachedNioReader(
                    inputChannel, approxMemoryLimit, raCacheCount, RandomAccessCachedNioReader.CachePolicy.Type.RR);

            ByteBuffer tmpBuffer = ByteBuffer.allocate(8);
            byte[] newLineArray = new byte[]{'\n'};

            for (ByteBuffer sortedIndexBuffer = ByteBuffer.allocate(entryLen);
                 sortedIndexByteChannel.read(sortedIndexBuffer) != -1;
            ) {
                // get row number
                long rowNum = sortedIndexBuffer.flip().position(entryLen - 8).getLong();
                sortedIndexBuffer.clear();

                // get offset
                int wtf = offsetsChannel.position(rowNum * 8).read(tmpBuffer);
                if (wtf != 8) {
                    throw new IllegalStateException("cannot read offset end");
                }
                long offsetEnd = tmpBuffer.flip().getLong();
                tmpBuffer.clear();
                long offsetStart = 0;
                if (rowNum != 0) {
                    if (offsetsChannel.position((rowNum - 1) * 8).read(tmpBuffer) != 8) {
                        throw new IllegalStateException("cannot read offset start");
                    }
                    offsetStart = tmpBuffer.flip().getLong();
                    tmpBuffer.clear();
                }
                // read from offset and write to output
                // note: exclude linebreak
                if (offsetStart != 0) {
                    offsetStart += 1;
                }
                byte[] data = new byte[(int) (offsetEnd - offsetStart)];
                raSourceReader.read(data, offsetStart);
                outputWriter.write(data);
                outputWriter.write(newLineArray);
            }
        }
    }

    private static void doTwoPassMerge(
            long approxMemoryLimit,
            int writingCacheSize,
            Path indexBinaryFile,
            Path sortedIndexBinaryFile,
            SortDescription indexSortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> indexBinParams,
            int entryLen,
            long batchSize
    ) throws IOException {
        try (
                SeekableByteChannel indexBinChannel = Files.newByteChannel(indexBinaryFile, StandardOpenOption.READ);
                SeekableByteChannel sortedIndexBinChannel = Files.newByteChannel(sortedIndexBinaryFile, StandardOpenOption.WRITE)
        ) {
            ExternalKwayMerge merger = new ExternalKwayMerge(indexBinParams, indexSortDesc);
            long indexSize = indexBinChannel.size();
            long presortedLen = Math.min(indexSize, batchSize * entryLen);
            merger.doublePassKwayMerge(
                    indexBinChannel,
                    sortedIndexBinChannel,
                    approxMemoryLimit,
                    presortedLen,
                    (int) Math.sqrt(indexSize / entryLen), // integer division will be precise
                    writingCacheSize
            );
        }
    }

    private static void doBatchSortAndBinarize(
            Path indexBinaryFile,
            SortDescription indexSortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> indexBinParams,
            EnumeratorCSVReader enumerator,
            long batchSize
    ) throws IOException, BrokenContentsException {
        // __approximate__ memory limit
        ExternalKwayMerge.BatchSorterCSVReader batchSorter = new ExternalKwayMerge.BatchSorterCSVReader(
                enumerator, batchSize, indexSortDesc.toRowComparator()
        );
        CSVFileBinarizer.binarize(batchSorter, indexBinParams, indexBinaryFile);
    }

}