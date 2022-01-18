package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.ManualByteBufferAllocator;
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

    // ---------------------------------
    // full pipeline
    // ---------------------------------

    // use the entire pipeline (for the largest files)
    public static void sort_maximum(
            Path inputCsvFile,
            CSVReader inputCsvReader,
            SortDescription sortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> indexBinParams, // only for key columns (!)
            Path outputCsvFile,
            long approxMemoryLimit,
            int writingCacheSize, // also used as reading cache size when necessary
            int raCacheCount,
            int batchSortSize
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
//        List<CSVFileBinarizer.ColumnTypeBinarizationParams> indexBinParams = Utility.slice(binParams, toCut);
        indexBinParams = new ArrayList<>(indexBinParams); // modifiable
        indexBinParams.add(new CSVFileBinarizer.LongCTBS()); // enumeration

        // inputReader -> cut -> enumerate = create index, add enumeration param
        CuttingCSVReader cutter = new CuttingCSVReader(inputCsvReader, toCut);
        EnumeratorCSVReader enumerator = new EnumeratorCSVReader(cutter);

        // !!!
        // stages that consume memory: batchSort, merge, permute
        // they should be separated to different functions for GC to work
        // !!!

        int entryLen = CSVFileBinarizer.calcSumByteLength(indexBinParams);

        // batchSort input and binarize
        doBatchSortAndBinarize(indexBinaryFile, indexSortDesc, indexBinParams, enumerator, batchSortSize);

        // merge index
        doTwoPassMerge(approxMemoryLimit, writingCacheSize, indexBinaryFile, sortedIndexBinaryFile, indexSortDesc, indexBinParams, entryLen, batchSortSize);

        // permute source = generate offsets + for (pos in index) { read offsets and write offset value to output }
        // in fact, no need to debinarize index
        int offsetsHeaderShift;
        try (
                FileChannel inputChannel = FileChannel.open(inputCsvFile, StandardOpenOption.READ);
                FileChannel offsetsChannel = FileChannel.open(offsetsFile, StandardOpenOption.WRITE)
        ) {
            // offsets
            offsetsHeaderShift = CSVOffsetExtractor.extractOffsets(
                    inputChannel,
                    offsetsChannel,
                    writingCacheSize,
                    writingCacheSize,
                    inputCsvReader.meta().hasNames()
            );
        }
        doPermuteAndWriteOutput(
                inputCsvFile,
                outputCsvFile,
                approxMemoryLimit,
                writingCacheSize,
                raCacheCount,
                sortedIndexBinaryFile,
                offsetsFile,
                entryLen,
                offsetsHeaderShift
        );

        ManualByteBufferAllocator.deallocate();
    }

    private static void doPermuteAndWriteOutput(
            Path inputCsvFile,
            Path outputCsvFile,
            long approxMemoryLimit,
            int writingCacheSize,
            int raCacheCount,
            Path sortedIndexBinaryFile,
            Path offsetsFile,
            int entryLen,
            int offsetHeaderShift
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

            // don't forget the header!
            if (offsetHeaderShift > 0) {
                ByteBuffer headerBuffer = ByteBuffer.allocate(offsetHeaderShift);
                inputChannel.read(headerBuffer);
                outputWriter.write(headerBuffer.array());
            }

            for (ByteBuffer sortedIndexBuffer = ByteBuffer.allocate(entryLen);
                 sortedIndexByteChannel.read(sortedIndexBuffer) != -1;
            ) {
                // get row number
                long rowNum = sortedIndexBuffer.flip().position(entryLen - 8).getLong();
                sortedIndexBuffer.clear();

                // get offset
                if (offsetsChannel.position(rowNum * 8).read(tmpBuffer) != 8) {
                    throw new IllegalStateException("cannot read offset end");
                }
                long offsetEnd = tmpBuffer.flip().getLong();
                tmpBuffer.clear();
                long offsetStart = offsetHeaderShift;
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

            System.out.println("RA miss rate: " + raSourceReader.missRate());
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
            Path binaryFile,
            SortDescription sortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams,
            CSVReader reader,
            int batchSize
    ) throws IOException, BrokenContentsException {
        // __approximate__ memory limit
        ExternalKwayMerge.BatchSorterCSVReader batchSorter = new ExternalKwayMerge.BatchSorterCSVReader(
                reader, batchSize, sortDesc.toRowComparator()
        );
        CSVFileBinarizer.binarize(batchSorter, binParams, binaryFile);
    }

    // ---------------------------------
    // binarize entire input
    // ---------------------------------

    // final permute() seems to be a catastrophic slowdown
    // instead let's binarize the entire input file

    public static void sort_binarize(
            CSVReader inputCsvReader,
            SortDescription sortDesc,
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> inputBinParams, // for all columns (!)
            long approxMemoryLimit,
            int writingCacheSize, // also used as reading cache size when necessary
            int batchSortSize,
            CSVWriter outputCsvWriter
    ) throws IOException, BrokenContentsException {
        FileManager fm = FileManager.createTempDirectory("sort");
        Path binaryFile = fm.createTempFile("binary");
        Path sortedBinaryFile = fm.createTempFile("sortedBinary");

        doBatchSortAndBinarize(
                binaryFile,
                sortDesc,
                inputBinParams,
                inputCsvReader,
                batchSortSize
        );

        doTwoPassMerge(
                approxMemoryLimit,
                writingCacheSize,
                binaryFile,
                sortedBinaryFile,
                sortDesc,
                inputBinParams,
                CSVFileBinarizer.calcSumByteLength(inputBinParams),
                batchSortSize
        );

        if (inputCsvReader.meta().hasNames()) {
            outputCsvWriter.write(inputCsvReader.meta().toRow());
        }

        CSVFileBinarizer.debinarize(sortedBinaryFile, inputBinParams, outputCsvWriter);
    }

}
