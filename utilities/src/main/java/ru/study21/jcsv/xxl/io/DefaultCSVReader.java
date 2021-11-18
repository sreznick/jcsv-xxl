package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public class DefaultCSVReader implements CSVReader {
    private BufferedReader _reader;
    private RecordSupplier _recordSupplier;
    private CSVMeta _meta;
    private List<String> _record;

    private DefaultCSVReader(BufferedReader reader, boolean withHeader, char separator)
            throws BrokenContentsException, IOException {
        _reader = reader;
        _recordSupplier = new RecordParser(separator);
        if (withHeader) {
            HeaderSupplier headerSupplier = new HeaderParser(separator);
            _meta = headerSupplier.parse(reader);
            _record = _recordSupplier.parse(reader);
        } else {
            _record = _recordSupplier.parse(reader);
            _meta = CSVMeta.withoutNames(_record.size());
        }
    }

    public static Builder builder(BufferedReader reader) {
        return new Builder(reader);
    }

    public CSVMeta meta() {
        return _meta;
    }

    public List<String> nextRow() throws BrokenContentsException {
        List<String> result = _record;
        try {
            _record = _recordSupplier.parse(_reader);
        } catch (IOException e) {
            // TODO: fix it
            throw new RuntimeException(e);
        }
        return result;
    }

    public static class Builder {
        private boolean _withHeader = false;
        private char _separator = ',';
        private final BufferedReader _reader;

        public Builder(BufferedReader reader) {
            _reader = reader;
            _withHeader = false;
            _separator = ',';
        }

        public Builder withHeader() {
            _withHeader = true;
            return this;
        }

        public Builder withoutHeader() {
            _withHeader = false;
            return this;
        }

        public Builder withSeparator(char c) {
            _separator = c;
            return this;
        }

        public Builder ofType(CSVType csvType) {
            return (csvType.withHeader() ? withHeader() : withoutHeader()).withSeparator(csvType.separator());
        }

        public DefaultCSVReader build() throws BrokenContentsException, IOException {
            return new DefaultCSVReader(_reader, _withHeader, _separator);
        }
    }
}
