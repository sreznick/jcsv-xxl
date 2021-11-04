package ru.study21.jcsv.xxl.analyzer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.Csv;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class CSVCustomizableAnalyzerTest {

    @Test
    void testSingleAction() {
        String text = "number\n1\n2\n3\n4\n";

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().build();
            CSVCustomizableAnalyzer analyzer = CSVCustomizableAnalyzer.builder(csvReader)
                    .addAction(new CSVCustomizableAnalyzer.Action<Integer>() {
                        int sum = 0;

                        @Override
                        public void acceptRow(List<String> row) {
                            sum += Integer.parseInt(row.get(0));
                        }

                        @Override
                        public Integer getResult() {
                            return sum;
                        }
                    }).build();

            List<?> result = analyzer.run();

            assertNotNull(result);
            assertIterableEquals(List.of(10), result);

        } catch (IOException | BrokenContentsException e) {
            Assertions.fail("Unexpected " + e);
        }
    }

    @Test
    void testMultipleActions() {
        String text = "number\n1\n2\n3\n4\n";

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().build();
            CSVCustomizableAnalyzer analyzer = CSVCustomizableAnalyzer.builder(csvReader)
                    .addAction(new CSVCustomizableAnalyzer.Action<Integer>() {
                        int sum = 0;

                        @Override
                        public void acceptRow(List<String> row) {
                            sum += Integer.parseInt(row.get(0));
                        }

                        @Override
                        public Integer getResult() {
                            return sum;
                        }
                    })
                    .addAction(new CSVCustomizableAnalyzer.Action<Integer>() {
                        int prod = 1;

                        @Override
                        public void acceptRow(List<String> row) {
                            prod *= Integer.parseInt(row.get(0));
                        }

                        @Override
                        public Integer getResult() {
                            return prod;
                        }
                    }).build();

            List<?> result = analyzer.run();

            assertNotNull(result);
            assertIterableEquals(List.of(10, 24), result);

        } catch (IOException | BrokenContentsException e) {
            Assertions.fail("Unexpected " + e);
        }
    }

    @Test
    void testSampleActions() {
        String text = "number,text\n1,d\n2,c\n3,b\n4,a\n";

        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            DefaultCSVReader csvReader = DefaultCSVReader.builder(reader).withHeader().build();
            CSVCustomizableAnalyzer analyzer = CSVCustomizableAnalyzer.builder(csvReader)
                    .addAction(CSVCustomizableAnalyzer.maxIntAction(0))
                    .addAction(CSVCustomizableAnalyzer.sumAction(0))
                    .addAction(CSVCustomizableAnalyzer.maxValuesAction(
                            1, 2, String::compareTo, Function.identity()))
                    .addAction(CSVCustomizableAnalyzer.averageAction(0))
                    .build();

            List<?> result = analyzer.run();

            assertNotNull(result);
            assertIterableEquals(List.of(4, new BigInteger("10"), List.of("d", "c"), 2.5), result);

        } catch (IOException | BrokenContentsException e) {
            Assertions.fail("Unexpected " + e);
        }
    }

}
