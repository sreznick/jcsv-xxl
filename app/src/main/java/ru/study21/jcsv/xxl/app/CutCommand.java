package ru.study21.jcsv.xxl.app;

import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.io.CuttingCSVReader;
import ru.study21.jcsv.xxl.io.DefaultCSVReader;
import ru.study21.jcsv.xxl.io.DefaultCSVWriter;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;

import static picocli.CommandLine.*;

@Command(name = "cut")
public class CutCommand implements Callable<Integer> {

    @ParentCommand
    private JCSVXXLApp parent;

    @Option(names = "-i", required = true)
    File inputFile;

    @Option(names = "-l", required = true)
    List<Integer> indices;

    @Option(names = "-o", required = true)
    File outputFile;

    @Override
    public Integer call() {
        PrintWriter err = parent.spec.commandLine().getErr();

        try (
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))
        ) {
            CuttingCSVReader cuttingReader = new CuttingCSVReader(
                    DefaultCSVReader.builder(br)
                            .setHeader(parent.withHeader)
                            .withSeparator(parent.separator)
                            .build(),
                    indices
            );
            DefaultCSVWriter writer = new DefaultCSVWriter(bw);

            if (parent.withHeader) {
                writer.write(cuttingReader.meta().toRow());
            }
            for (List<String> row = cuttingReader.nextRow(); !row.isEmpty(); row = cuttingReader.nextRow()) {
                writer.write(row);
            }

        } catch (BrokenContentsException e) {
            err.println("Broken file contents: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            err.println("IOException: " + e.getMessage());
            return 2;
        }
        return 0;
    }

}
