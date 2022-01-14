package ru.study21.jcsv.xxl.io;

import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.ToIntFunction;

public class CSVFileBinarizer {

    // supposed to support all types from ColumnType
    public static abstract class ColumnTypeBinarizationParams {
    }

    public static class IntCTBS extends ColumnTypeBinarizationParams {
        public IntCTBS() {
        }
    }

    public static class LongCTBS extends ColumnTypeBinarizationParams {
        public LongCTBS() {
        }
    }

    // if a number cannot fit into byteLength, an IllegalArgumentException is thrown
    public static class BigIntCTBS extends ColumnTypeBinarizationParams {
        protected final int byteLength;

        public BigIntCTBS(int byteLength) {
            if (byteLength > 256) {
                throw new IllegalArgumentException("such large numbers are not supported"); // TODO ?
                // 2^256 is basically # of atoms in the observable Universe
            }
            this.byteLength = byteLength;
        }
    }

    // if a string is longer than charsLength, it is cut to that length
    // if a string starts with NUL, it will be removed while reading from binary
    public static class StringCTBS extends ColumnTypeBinarizationParams {
        protected final int charsLength;
        protected final Charset tempCharset;
        protected final int bytesPerChar;

        public StringCTBS(int charsLength, Charset tempCharset) {
            this.charsLength = charsLength;
            this.tempCharset = tempCharset;
            // supported charsets must have same byte count for all chars
            this.bytesPerChar = Optional.of(tempCharset).map(c -> {
                if (
                        Set.of(
                                "US-ASCII", "ASCII", // standard ASCII-128
                                "IBM850", "Cp850", // ASCII-256 with extra Latin (European) symbols
                                "IBM866", "Cp866" // ASCII-256 with Cyrillic symbols
                        ).contains(c.name())) {
                    return 1;
                }
                return null;
            }).orElseThrow(() -> new IllegalArgumentException("unsupported charset " + tempCharset.name()));

        }
    }

    protected static byte[] addPaddingBigEndian(byte[] arr, int length) {
        if (arr.length > length) {
            throw new IllegalArgumentException("byte array longer than expected (" + arr.length + " > " + length + "); " +
                    "is a BigInteger too large?");
        }
        if (arr.length == length) {
            return arr;
        }
        int padding = length - arr.length;
        byte[] result = new byte[length];
        if (length - padding >= 0) {
            System.arraycopy(arr, 0, result, padding, arr.length);
        }
        return result;
    }

    protected static byte[] removePaddingBigEndian(byte[] arr) {
        int padding = 0;
        while (arr[padding] == 0) {
            padding++;
            if (padding == arr.length) {
                return new byte[]{0};
            }
        }
        byte[] result = new byte[arr.length - padding];
        System.arraycopy(arr, padding, result, 0, arr.length - padding);
        return result;
    }

    public static int calcSumByteLength(List<ColumnTypeBinarizationParams> params) {
        return calcOffset(params, params.size());
    }

    public static int calcOffset(List<ColumnTypeBinarizationParams> params, int index) {
        return params.stream().limit(index).mapToInt(
                new ToIntFunction<>() { // cannot throw from lambda
                    @Override
                    public int applyAsInt(ColumnTypeBinarizationParams params) {
                        if (params instanceof IntCTBS) {
                            return 4;
                        } else if (params instanceof LongCTBS) {
                            return 8;
                        } else if (params instanceof BigIntCTBS bigintParams) {
                            return bigintParams.byteLength + 1; // extra byte; see comments in `binarize()`
                        } else if (params instanceof StringCTBS stringParams) {
                            return stringParams.charsLength * stringParams.bytesPerChar;
                        }
                        throw new IllegalArgumentException("unknown params class");
                    }
                }
        ).sum();
    }

    public static void binarize(CSVReader reader, List<ColumnTypeBinarizationParams> binParams, Path binFile)
            throws IOException, BrokenContentsException {
        int rowSize = calcSumByteLength(binParams);

        try (WritableByteChannel binChannel = Files.newByteChannel(binFile, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocate(rowSize);
            // assumes no broken rows
            List<String> row;
            while ((row = reader.nextRow()).size() > 0) {
                if (row.size() != binParams.size()) {
                    throw new BrokenContentsException("wrong number of columns: " + row.size() + " != " + binParams.size());
                }
                Iterator<String> elemIter = row.iterator();
                Iterator<ColumnTypeBinarizationParams> paramsIter = binParams.iterator();
                while (elemIter.hasNext() /* && paramsIter.hasNext() */) {
                    String elem = elemIter.next();
                    ColumnTypeBinarizationParams params = paramsIter.next();

                    if (params instanceof IntCTBS) {
                        buffer.putInt(Integer.parseInt(elem));

                    } else if (params instanceof LongCTBS) {
                        buffer.putLong(Long.parseLong(elem));

                    } else if (params instanceof BigIntCTBS bigIntCTBS) {
                        // unfortunately [0, -32] <-> 224 and [-32] <-> -32
                        // therefore simple padding is impossible
                        // solution: add extra byte that determines padding
                        byte[] arr = new BigInteger(elem).toByteArray();
                        buffer.put((byte) (arr.length - 128));
                        buffer.put(addPaddingBigEndian(arr, bigIntCTBS.byteLength));

                    } else if (params instanceof StringCTBS stringCTBS) {
                        Charset cset = stringCTBS.tempCharset;
                        if (elem.length() > stringCTBS.charsLength) {
                            elem = elem.substring(0, stringCTBS.charsLength);
                        }
                        ByteBuffer elemBuf = cset.encode(elem);
                        byte[] elemArr = new byte[elemBuf.remaining()];
                        elemBuf.get(elemArr);
                        int byteLength = stringCTBS.charsLength * stringCTBS.bytesPerChar;
                        buffer.put(addPaddingBigEndian(elemArr, byteLength));

                    } else {
                        throw new IllegalArgumentException("unknown params class " + params.getClass().getName());
                    }
                }
                buffer.flip();
                binChannel.write(buffer);
                buffer.clear();
            }
        }
    }

    public static void debinarize(Path binFile, List<ColumnTypeBinarizationParams> binParams, CSVWriter csvWriter)
            throws IOException, BrokenContentsException {
        int rowSize = calcSumByteLength(binParams);

        try (ReadableByteChannel binChannel = Files.newByteChannel(binFile, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(rowSize);
            while (binChannel.read(buffer) != -1) {
                buffer.flip();
                List<String> row = new ArrayList<>();
                for (ColumnTypeBinarizationParams params : binParams) {
                    if (params instanceof IntCTBS) {
                        row.add(String.valueOf(buffer.getInt()));

                    } else if (params instanceof LongCTBS) {
                        row.add(String.valueOf(buffer.getLong()));

                    } else if (params instanceof BigIntCTBS bigIntCTBS) {
                        int trueByteLength = buffer.get() + 128;
                        byte[] arr = new byte[bigIntCTBS.byteLength];
                        buffer.get(arr);
                        byte[] trueArr = addPaddingBigEndian(removePaddingBigEndian(arr), trueByteLength);
                        row.add(new BigInteger(trueArr).toString());

                    } else if (params instanceof StringCTBS stringCTBS) {
                        int byteLength = stringCTBS.charsLength * stringCTBS.bytesPerChar;
                        byte[] arr = new byte[byteLength];
                        buffer.get(arr);
                        Charset cset = stringCTBS.tempCharset;
                        byte[] decodedArr = removePaddingBigEndian(arr);
                        row.add(cset.decode(ByteBuffer.wrap(decodedArr)).toString());

                    } else {
                        throw new IllegalArgumentException("unknown params class " + params.getClass().getName());
                    }
                }
                csvWriter.write(row);
                buffer.clear();
            }
        }
    }

}
