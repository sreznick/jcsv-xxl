package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExternalKwayMerge {

    protected static class BatchSorterCSVReader implements CSVReader {

        private final CSVReader delegate;
        private final int batchSize;
        private final Comparator<List<String>> rowCmp;

        private final List<List<String>> batch;
        private Iterator<List<String>> batchIter = null;

        public BatchSorterCSVReader(CSVReader delegate, int batchSize, Comparator<List<String>> rowCmp) {
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
    private final int rowSize;

    public ExternalKwayMerge(
            List<CSVFileBinarizer.ColumnTypeBinarizationParams> binParams,
            SortDescription sortDescription
    ) {
        this.binParams = binParams;
        this.rowSize = CSVFileBinarizer.calcSumByteLength(binParams);
        this.sortDescription = sortDescription;
    }

    protected void singlePassKwayMerge(
            MultiregionCachedNioBinaryReader reader,
            CachedNioBinaryWriter writer,
            int regionCount,
            int entryLen
    ) throws IOException {

        // initialize variables
        List<byte[]> entries = new ArrayList<>(regionCount);

        List<Integer> offsets = new ArrayList<>(binParams.size());
        for (int index = 0; index < binParams.size(); index++) {
            // TODO this is O(binParams.size ^ 2), can do better (but does it matter?)
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
        assert cnt == reader.regionLenSum() / entryLen;
    }


}
