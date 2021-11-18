package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;
import ru.study21.jcsv.xxl.algorithms.InMemorySorter;
import ru.study21.jcsv.xxl.algorithms.SortDescription;
import ru.study21.jcsv.xxl.analyzer.CSVBasicAnalyzer;
import ru.study21.jcsv.xxl.analyzer.CSVSummary;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVMeta;
import ru.study21.jcsv.xxl.common.CSVType;
import ru.study21.jcsv.xxl.data.CSVTable;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.ParentCommand;

@Command(name = "sort")
public class SortCommand implements Callable<Integer> {

    /*
     * TODO:
     *  - check size and base sorting strategy on it
     *  - output to file
     */
    @ParentCommand
    private JCSVXXLApp parent;

    @CommandLine.Parameters(arity = "1..*")
    List<File> files;

    @Override
    public Integer call() {
        PrintWriter out = parent.spec.commandLine().getOut();
        PrintWriter err = parent.spec.commandLine().getErr();

        for (File file : files) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                InMemorySorter sorter = new InMemorySorter(SortDescription.of(SortDescription.KeyElement.asString(0)));
                CSVTable table = CSVTable.load(br, new CSVType(parent.withHeader, parent.separator));

                CSVTable sorted = sorter.sorted(table);

                CSVMeta meta = sorted.meta();
                if (meta.hasNames()) {
                    for (int i = 0; i < meta.size() - 1; ++i) {
                        System.out.print(meta.columnName(i));
                        System.out.print(parent.separator);
                    }
                    System.out.print(meta.columnName(meta.size() - 1));
                    System.out.println();
                }
                for (int i = 0; i < sorted.size(); ++i) {
                    for (int j = 0; j < meta.size() - 1; ++j) {
                        System.out.print(sorted.cell(i, j));
                        System.out.print(parent.separator);
                    }
                    System.out.print(sorted.cell(i, meta.size() - 1));
                    System.out.println();
                }
            } catch (BrokenContentsException e) {
                err.println("Broken file contents: " + e.getMessage());
                return 1;
            } catch (IOException e) {
                err.println("IOException: " + e.getMessage());
                return 2;
            }

        }

        return 0;
    }

}
