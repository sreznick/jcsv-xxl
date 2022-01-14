package ru.study21.jcsv.xxl.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CSVFileBinarizerPaddingTest {

    @Test
    public void testNoPadding() {
        assertArrayEquals(
                new byte[]{1, 2, 3, 4},
                CSVFileBinarizer.addPaddingBigEndian(new byte[]{1, 2, 3, 4}, 4)
        );
        assertArrayEquals(
                new byte[]{1},
                CSVFileBinarizer.addPaddingBigEndian(new byte[]{1}, 1)
        );
        assertArrayEquals(
                new byte[]{1, 2, 3, 4},
                CSVFileBinarizer.removePaddingBigEndian(new byte[]{1, 2, 3, 4})
        );
        assertArrayEquals(
                new byte[]{1},
                CSVFileBinarizer.removePaddingBigEndian(new byte[]{1})
        );
    }

    @Test
    public void testPadding() {
        assertArrayEquals(
                new byte[]{0, 1, 2, 3},
                CSVFileBinarizer.addPaddingBigEndian(new byte[]{1, 2, 3}, 4)
        );
        assertArrayEquals(
                new byte[]{0, 0, 0, 0, 1},
                CSVFileBinarizer.addPaddingBigEndian(new byte[]{1}, 5)
        );
        assertArrayEquals(
                new byte[]{1, 2, 3},
                CSVFileBinarizer.removePaddingBigEndian(new byte[]{0, 1, 2, 3})
        );
        assertArrayEquals(
                new byte[]{1},
                CSVFileBinarizer.removePaddingBigEndian(new byte[]{0, 0, 0, 0, 1})
        );
        assertArrayEquals(
                new byte[]{0},
                CSVFileBinarizer.removePaddingBigEndian(new byte[]{0, 0, 0})
        );
    }

}
