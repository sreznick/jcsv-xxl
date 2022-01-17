package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.*;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class ExternalKwayMerge {

    protected static class BatchSorterCSVReader implements CSVReader {

        private final CSVReader delegate;
        private final long batchSize;
        private final Comparator<List<String>> rowCmp;

        private final List<List<String>> batch;
        private Iterator<List<String>> batchIter = null;

        public BatchSorterCSVReader(CSVReader delegate, long batchSize, Comparator<List<String>> rowCmp) {
            this.delegate = delegate;
            this.batchSize = batchSize;
            this.rowCmp = rowCmp;
            this.batch = new ArrayList<>();
        }

        @Override
        public CSVMeta meta() {
            return delegate.meta();
        }

        @Override
        public List<String> nextRow() throws BrokenContentsException {
            if (batchIter != null && batchIter.hasNext()) {
                return batchIter.next();
            }
            batch.clear();
            List<String> row;
            while (batch.size() < batchSize) {
                row = delegate.nextRow();
                if (row.size() == 0) {
                    break;
                }
                batch.add(row);
            }
            if (batch.isEmpty()) {
                return List.of();
            }
            batch.sort(rowCmp); // in-place for ArrayList!
            batchIter = batch.listIterator();
            return batchIter.next();
        }
    }

    private final List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams;
    private final SortDescription sortDescription;
    private final int entryLen;

    public ExternalKwayMerge(
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams,
            SortDescription sortDescription
    ) {
        this.binParams = binParams;
        this.entryLen = CSVFileBinarizer.calcSumByteLength(binParams);
        this.sortDescription = sortDescription;
    }

    public void singlePassMerge(
            CSVReader input,
            CSVWriter output,
            long approxMemoryLimit,
            int writingCacheSize
    ) throws IOException, BrokenContentsException {
        sortAndMergeWrapper(
                input, output, approxMemoryLimit, new MergeRunnable() {
                    @Override
                    public void run(
                            SeekableByteChannel binInputChannel,
                            SeekableByteChannel binOutputChannel,
                            long presortedLen
                    ) throws IOException {
                        singlePassKwayMerge(
                                binInputChannel,
                                binOutputChannel,
                                approxMemoryLimit,
                                presortedLen,
                                writingCacheSize
                        );
                    }
                }
        );
    }

    public void doublePassMerge(
            CSVReader input,
            CSVWriter output,
            long approxMemoryLimit,
            int k1,
            int writingCacheSize
    ) throws BrokenContentsException, IOException {
        sortAndMergeWrapper(
                input, output, approxMemoryLimit, new MergeRunnable() {
                    @Override
                    public void run(
                            SeekableByteChannel binInputChannel,
                            SeekableByteChannel binOutputChannel,
                            long presortedLen
                    ) throws IOException {
                        doublePassKwayMerge(
                                binInputChannel,
                                binOutputChannel,
                                approxMemoryLimit,
                                presortedLen,
                                k1,
                                writingCacheSize
                        );
                    }
                }
        );
    }

    // set optimal k1 automatically (sqrt of entry count)
    public void doublePassMerge(
            CSVReader input,
            CSVWriter output,
            long approxMemoryLimit,
            int writingCacheSize
    ) throws BrokenContentsException, IOException {
        sortAndMergeWrapper(
                input, output, approxMemoryLimit, new MergeRunnable() {
                    @Override
                    public void run(
                            SeekableByteChannel binInputChannel,
                            SeekableByteChannel binOutputChannel,
                            long presortedLen
                    ) throws IOException {
                        doublePassKwayMerge(
                                binInputChannel,
                                binOutputChannel,
                                approxMemoryLimit,
                                presortedLen,
                                (int) Math.sqrt(binInputChannel.size() / entryLen), // integer division will be precise
                                writingCacheSize
                        );
                    }
                }
        );
    }

    // ----------- private methods -------------

    private interface MergeRunnable {
        void run(
                SeekableByteChannel binInputChannel,
                SeekableByteChannel binOutputChannel,
                long presortedLen
        ) throws IOException;
    }

    protected void sortAndMergeWrapper(
            CSVReader input,
            CSVWriter output,
            long approxMemoryLimit,
            MergeRunnable mergeBlock
    ) throws IOException, BrokenContentsException {
        FileManager fm = FileManager.createTempDirectory("merge");
        Path tempBinInput = fm.createTempFile("binInput");
        Path tempBinOutput = fm.createTempFile("binOutput");

        // TODO: check if this already is a sorting reader
        // will need to know actual presortedLen, which by default is unknown until end of file
        // (presortedLen = binParams.len * batchSize

        // __approximate__ memory limit
        long batchSize = approxMemoryLimit / entryLen;
        input = new BatchSorterCSVReader(input, batchSize, sortDescription.toRowComparator());

        CSVFileBinarizer.binarize(input, binParams, tempBinInput);

        try (SeekableByteChannel binInputChannel = Files.newByteChannel(tempBinInput, StandardOpenOption.READ);
             SeekableByteChannel binOutputChannel = Files.newByteChannel(tempBinOutput, StandardOpenOption.WRITE)
        ) {
            long presortedLen = Math.min(binInputChannel.size(), batchSize * entryLen);
            mergeBlock.run(binInputChannel, binOutputChannel, presortedLen);
        }

        CSVFileBinarizer.debinarize(tempBinOutput, binParams, output);
    }

    protected void singlePassKwayMerge(
            SeekableByteChannel binInputChannel,
            SeekableByteChannel binOutputChannel,
            long approxMemLimit,
            long presortedLen,
            int writingCacheSize
    ) throws IOException {

        approxMemLimit -= writingCacheSize;
        if (approxMemLimit < 0) {
            throw new IllegalArgumentException("not enough memory provided");
        }

        // setup reader
        long inputSize = binInputChannel.size();
        // TODO: next line might be the cause of OOM if too many columns
        // although for binary files #columns is supposed to be low
        int singleRegionCacheSize = Math.max((int) (inputSize / approxMemLimit), 1024);
        List<MultiregionCachedNioBinaryReader.Region> regions = new ArrayList<>((int) (inputSize / presortedLen));
        for (long r = 0; r <= inputSize - presortedLen; r += presortedLen) {
            regions.add(new MultiregionCachedNioBinaryReader.Region(r, presortedLen));
        }
        if (inputSize % presortedLen != 0) {
            long lastLen = inputSize % presortedLen;
            regions.add(new MultiregionCachedNioBinaryReader.Region(inputSize - lastLen, lastLen));
        }

        try (
                MultiregionCachedNioBinaryReader reader = new MultiregionCachedNioBinaryReader(binInputChannel, regions, singleRegionCacheSize);
                CachedNioBinaryWriter writer = new CachedNioBinaryWriter(binOutputChannel, writingCacheSize)
        ) {
            // go go go
            doKwayMerge(
                    reader,
                    writer,
                    regions.size()
            );
        }
    }

    protected void doublePassKwayMerge(
            SeekableByteChannel binInputChannel,
            SeekableByteChannel binOutputChannel,
            long approxMemLimit,
            long presortedLen,
            int k1, // k for first pass
            int writingCacheSize
    ) throws IOException {
        approxMemLimit -= writingCacheSize;
        if (approxMemLimit < 0) {
            throw new IllegalArgumentException("not enough memory provided");
        }
        Path tempFile = FileManager.createTempDirectory("singlePassKwayMerge").createTempFile("temp");

        // first pass writes to temporary file
        try (SeekableByteChannel tempChannel = Files.newByteChannel(tempFile, StandardOpenOption.WRITE)) {
            long inputSize = binInputChannel.size();
            int readCacheSize = (int) (approxMemLimit / k1);

            for (long firstPassStart = 0; firstPassStart < inputSize; firstPassStart += k1 * presortedLen) {
                // setup reader
                List<MultiregionCachedNioBinaryReader.Region> regions = new ArrayList<>(k1);
                long partEnd = Math.min(inputSize, firstPassStart + k1 * presortedLen);
                for (long r = firstPassStart; r <= partEnd - presortedLen; r += presortedLen) {
                    regions.add(new MultiregionCachedNioBinaryReader.Region(r, presortedLen));
                }
                if (partEnd % presortedLen != 0) {
                    long lastLen = partEnd % presortedLen;
                    regions.add(new MultiregionCachedNioBinaryReader.Region(partEnd - lastLen, lastLen));
                }

                try (
                        MultiregionCachedNioBinaryReader reader = new MultiregionCachedNioBinaryReader(binInputChannel, regions, readCacheSize);
                        CachedNioBinaryWriter writer = new CachedNioBinaryWriter(tempChannel, writingCacheSize)
                ) {
                    doKwayMerge(reader, writer, regions.size());
                }

            }
        }

        // now the second pass is simply a single pass with new params
        try (SeekableByteChannel tempChannel = Files.newByteChannel(tempFile, StandardOpenOption.READ)) {
            singlePassKwayMerge(
                    tempChannel,
                    binOutputChannel,
                    approxMemLimit,
                    Math.min(tempChannel.size(), k1 * presortedLen),
                    writingCacheSize
            );
        }
    }

    // triple pass is basically never practical

    protected void doKwayMerge(
            MultiregionCachedNioBinaryReader reader,
            CachedNioBinaryWriter writer,
            int regionCount
    ) throws IOException {

        // initialize variables
        List<byte[]> entries = new ArrayList<>(regionCount);

        List<Integer> offsets = new ArrayList<>(binParams.size());
        for (int index = 0; index < binParams.size(); index++) {
            // TODO this is O(binParams.size ^ 2), can do linear (but does it matter?)
            offsets.add(CSVFileBinarizer.calcOffset(binParams, index));
        }

        boolean[] isOver = new boolean[regionCount];

        // create comparator for heap

        // TODO: do not create a lot of temporary objects; compare bytes directly (tricky for two's complement)
        ByteComparator byteComparator = new SimpleByteComparator();

        Comparator<Integer> regionComparator = (i, j) -> {
            // regions that are over should be taken out of heap the last (this is the ending condition)
            if (isOver[i] && isOver[j]) {
                return 0;
            } else if (isOver[i]) {
                return 1;
            } else if (isOver[j]) {
                return -1;
            }
            for (SortDescription.KeyElement key : sortDescription.keys()) {
                boolean invertResult = key.order() == SortDescription.Order.DESCENDING;
                int result; // = 0
                byte[] e1 = entries.get(i);
                byte[] e2 = entries.get(j);
                int offset = offsets.get(key.field());
                CSVFileBinarizer.ColumnTypeBinarizationParams type = binParams.get(key.field());
                result = byteComparator.compare(type, e1, offset, e2, offset);
                if (result == 0) {
                    continue;
                }
                return invertResult ? -result : result;
            }
            return 0;
        };
        PairingHeap<Integer> heap = new PairingHeap<>(regionComparator);

        // initialize values
        for (int region = 0; region < regionCount; region++) {
            entries.add(new byte[entryLen]);
            if (!reader.isOver(region)) {
                reader.read(entries.get(region), region);
            } else {
                isOver[region] = true;
            }
        }
        heap.addAll(IntStream.range(0, regionCount).boxed().toList());

        // poll heap until done
        long cnt = 0;
        for (int r = heap.pollMin(); !isOver[r]; r = heap.pollMin(), cnt++) {
            writer.write(entries.get(r));
            if (reader.isOver(r)) {
                isOver[r] = true;
            } else {
                reader.read(entries.get(r), r);
            }
            heap.add(r); // updates heap
        }
        // sanity check (did fail...)
        assert cnt == reader.regionLenSum() / entryLen;
    }


}
