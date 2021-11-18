package ru.study21.jcsv.xxl.app;

import ru.study21.jcsv.xxl.analyzer.CSVBasicAnalyzer;
import ru.study21.jcsv.xxl.analyzer.CSVSummary;
import ru.study21.jcsv.xxl.analyzer.ColumnSummary;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.io.CSVReader;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static java.util.stream.IntStream.range;
import static picocli.CommandLine.*;

@Command(name = "summary")
public class SummaryCommand implements Callable<Integer> {

    @ParentCommand
    private JCSVXXLApp parent;

    @Parameters(arity = "1..*")
    List<File> files;

    @Override
    public Integer call() {
        PrintWriter out = parent.spec.commandLine().getOut();
        PrintWriter err = parent.spec.commandLine().getErr();

        for (File file : files) {
            out.println("--- Analyzing file " + file.getName() + " ---");
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                DefaultCSVReader.Builder readerBuilder = DefaultCSVReader.builder(br);
                if (parent.withHeader) {
                    readerBuilder.withHeader();
                } else {
                    readerBuilder.withoutHeader();
                }
                readerBuilder.withSeparator(parent.separator);

                CSVBasicAnalyzer analyzer = new CSVBasicAnalyzer(readerBuilder.build());
                CSVSummary summary = analyzer.run();

                out.println("File summary:");
                out.println("\tRows total:\t" + summary.getNRows());

                String tableFormat = "| " + ("%-15s | ".repeat(summary.getMeta().size())) + "%n";
                CSVMeta meta = summary.getMeta();
                Object[] columnNames = IntStream.range(0, meta.size()).mapToObj(meta::columnName).toArray();
                Object[] columnTypes = summary.getColumns().stream()
                        .map(columnSummary -> columnSummary.getType().toString()).toArray();

                out.println("Columns summary:");

                out.format("\t%-15s", "Name");
                out.format(tableFormat, columnNames);

                out.format("\t%-15s", "Deduced type");
                out.format(tableFormat, columnTypes);

            } catch (BrokenContentsException e) {
                err.println("Broken file contents: " + e.getMessage());
                return 1;
            } catch (IOException e) {
                err.println("IOException: " + e.getMessage());
                return 2;
            }
            out.println();
        }
        return 0;
    }

}
