package ru.study21.jcsv.xxl.algorithms;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.CSVFileBinarizer;
import ru.study21.jcsv.xxl.io.CSVReader;
import ru.study21.jcsv.xxl.io.CachedNioBinaryWriter;
import ru.study21.jcsv.xxl.io.MultiregionCachedNioBinaryReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.*;

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

}
