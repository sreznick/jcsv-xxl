package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.study21.jcsv.xxl.common.BrokenContentsException;

import java.util.List;

public class EnumeratorCSVReaderTest {

    @Test
    public void basicTest() throws BrokenContentsException {
        DummyCSVReader dummy = new DummyCSVReader(List.of(
                List.of("a"), List.of("b")
        ));
        EnumeratorCSVReader enumerator = new EnumeratorCSVReader(dummy);
        Assertions.assertEquals(List.of("a", "0"), enumerator.nextRow());
        Assertions.assertEquals(List.of("b", "1"), enumerator.nextRow());
        Assertions.assertEquals(List.of(), enumerator.nextRow());
    }

}
