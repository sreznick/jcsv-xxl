package ru.study21.jcsv.xxl.io;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static ru.study21.jcsv.xxl.io.CSVFileBinarizer.addPaddingBigEndian;
import static ru.study21.jcsv.xxl.io.CSVFileBinarizer.removePaddingBigEndian;

// compares by deserialization
// probably it is more efficient to compare bytes directly, but that is tricky
public class SimpleByteComparator implements ByteComparator {

    private final ByteBuffer temp = ByteBuffer.allocate(8);

    protected int parseInt(byte[] arr, int offset) {
        return temp.clear().put(arr, offset, 4).flip().getInt();
    }

    protected long parseLong(byte[] arr, int offset) {
        return temp.clear().put(arr, offset, 8).flip().getLong();
    }

    protected BigInteger parseBigInt(byte[] arr, int offset, int byteLength) {
        int trueByteLength = arr[offset] + 128;
        byte[] slice = new byte[byteLength];
        System.arraycopy(arr, offset + 1, slice, 0, byteLength);

        byte[] trueArr = addPaddingBigEndian(removePaddingBigEndian(slice), trueByteLength);
        return new BigInteger(trueArr);
    }

    protected String parseString(byte[] arr, int offset, CSVFileBinarizer.StringCTBS stringCTBS) {
        int byteLength = stringCTBS.charsLength * stringCTBS.bytesPerChar;
        byte[] slice = new byte[byteLength];
        System.arraycopy(arr, offset, slice, 0, byteLength);

        Charset cset = stringCTBS.tempCharset;
        byte[] decodedArr = removePaddingBigEndian(slice);
        return cset.decode(ByteBuffer.wrap(decodedArr)).toString();
    }

    @Override
    public int compare(CSVFileBinarizer.ColumnTypeBinarizationParams type,
                       byte[] arr1, int offset1,
                       byte[] arr2, int offset2) {
        if (type instanceof CSVFileBinarizer.IntCTBS) {
            return parseInt(arr1, offset1) - parseInt(arr2, offset2);
        } else if (type instanceof CSVFileBinarizer.LongCTBS) {
            long res = parseLong(arr1, offset1) - parseLong(arr2, offset2);
            // need to return int
            if (res < 0) {
                return -1;
            } else if (res > 0) {
                return 1;
            }
            return 0;
        } else if (type instanceof CSVFileBinarizer.BigIntCTBS bigIntCTBS) {
            int byteLength = bigIntCTBS.byteLength;
            return parseBigInt(arr1, offset1, byteLength).compareTo(parseBigInt(arr2, offset2, byteLength));
        } else if (type instanceof CSVFileBinarizer.StringCTBS stringCTBS) {
            return parseString(arr1, offset1, stringCTBS).compareTo(parseString(arr2, offset2, stringCTBS));
        }
        throw new IllegalArgumentException("unknown type class " + type.getClass().getName());
    }
}
