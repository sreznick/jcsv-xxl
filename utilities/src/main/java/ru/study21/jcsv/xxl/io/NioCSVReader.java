package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;

// for now only supports single-byte encoded charsets
public class NioCSVReader implements CSVReader {

    private final ByteBuffer buffer;
    private final SeekableByteChannel inputChannel;
    private final char separator;
    private final CSVMeta meta;

    public NioCSVReader(
            SeekableByteChannel inputChannel,
            int cacheSize,
            boolean withHeader,
            char separator
    ) throws BrokenContentsException, IOException {
        buffer = ByteBuffer.allocate(cacheSize);
        this.inputChannel = inputChannel;
        this.separator = separator;

        if (withHeader) {
            meta = CSVMeta.withNames(nextRow());
        } else {
            long pos = inputChannel.position();
            meta = CSVMeta.withoutNames(nextRow().size());
            inputChannel.position(pos);
            buffer.clear();
            buffer.flip(); // lim = pos = 0
            rem = 0;
        }
    }

    public NioCSVReader(
            SeekableByteChannel inputChannel,
            int cacheSize,
            boolean withHeader
    ) throws BrokenContentsException, IOException {
        this(inputChannel, cacheSize, withHeader, ',');
    }

    public NioCSVReader(
            SeekableByteChannel inputChannel,
            int cacheSize,
            char separator
    ) throws BrokenContentsException, IOException {
        this(inputChannel, cacheSize, false, separator);
    }

    public NioCSVReader(
            SeekableByteChannel inputChannel,
            int cacheSize
    ) throws BrokenContentsException, IOException {
        this(inputChannel, cacheSize, false, ',');
    }

    @Override
    public CSVMeta meta() {
        if (meta == null) {
            throw new IllegalStateException("meta unknown, no reads yet");
        }
        return meta;
    }

    @Override
    public List<String> nextRow() throws BrokenContentsException {
        if (eof) {
            return List.of();
        }

        List<String> result = new ArrayList<>(100); // profiled
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        boolean lastSymbolQuote = false;
        boolean lastSymbolSeparator = false;

        try {
            while (true) {
                byte c = readChar();
                if (eof) {
                    if (escaping) {
                        throw new BrokenContentsException("EOF before escaped block was terminated");
                    }
                    if (sb.length() != 0) {
                        result.add(sb.toString());
                    }
                    break;
                }
                if (c == '"') {
                    lastSymbolSeparator = false;
                    if (lastSymbolQuote) {
                        sb.append('"');
                        lastSymbolQuote = false;
                    } else {
                        lastSymbolQuote = true;
                    }
                    continue;
                }
                if (lastSymbolQuote) {
                    escaping = !escaping;
                }
                lastSymbolQuote = false;
                if (c == '\n' || c == '\r') {
                    if (lastSymbolSeparator) {
                        System.out.println(result);
                        System.out.println(sb);
                        throw new BrokenContentsException("last entry in row should not be followed by a separator");
                    }
//                    lastSymbolSeparator = false; // definitely false here
                    if (escaping) {
                        sb.append((char) c);
                    } else {
                        if(c == '\r') {
                            if(readChar() != '\n') {
                                throw new BrokenContentsException("CR line break is not supported");
                            }
                        }
                        result.add(sb.toString());
                        sb.delete(0, sb.length());
                        break;
                    }
                    continue;
                }
                if (c == separator) {
                    if (escaping) {
                        sb.append(separator);
                    } else {
                        result.add(sb.toString());
                        sb.delete(0, sb.length());
                        lastSymbolSeparator = true;
                        continue;
                    }
                }
                lastSymbolSeparator = false;
                sb.append((char) c);
            }
            if (meta != null && result.size() != meta.size() && result.size() != 0) {
                throw new BrokenContentsException("expected " + meta.size() + " columns but got " + result.size());
            }
            return result;

        } catch (IOException e) {
            BrokenContentsException ex = new BrokenContentsException("IO exception happened");
            ex.addSuppressed(e);
            throw ex;
        }
    }

    private int rem = 0;
    private boolean eof = false;

    private byte readChar() throws IOException {
        if (rem == 0) {
            buffer.clear();
            rem = inputChannel.read(buffer);
            buffer.flip();
        }
        if (rem == -1) {
            eof = true;
            return 0;
        }
        byte c = buffer.get();
        rem--;
        return c;
    }

}
