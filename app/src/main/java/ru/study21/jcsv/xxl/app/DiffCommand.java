package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import ru.study21.jcsv.xxl.algorithms.MyersDiff;
import ru.study21.jcsv.xxl.algorithms.SortDescription.KeyType;
import ru.study21.jcsv.xxl.common.BrokenContentsException;
import ru.study21.jcsv.xxl.common.CSVType;
import ru.study21.jcsv.xxl.data.CSVTable;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "diff")
public class DiffCommand implements Callable<Integer> {

    @ParentCommand
    private JCSVXXLApp parent;

    @CommandLine.Parameters()
    List<File> files;

    @CommandLine.Option(names = "-e")
    boolean editScript = false;

    @CommandLine.Option(names = "-c")
    List<Integer> columnNumbers;

    @Override
    public Integer call() {
        PrintWriter out = parent.spec.commandLine().getOut();
        PrintWriter err = parent.spec.commandLine().getErr();

        try (BufferedReader firstBR = new BufferedReader(new FileReader(files.get(0)));
             BufferedReader secondBR = new BufferedReader(new FileReader(files.get(1)))) {

            CSVTable firstTable = CSVTable.load(firstBR, new CSVType(parent.withHeader, parent.separator));
            CSVTable secondTable = CSVTable.load(secondBR, new CSVType(parent.withHeader, parent.separator));

            MyersDiff myersDiff = new MyersDiff(List.of(new MyersDiff.KeyInfo(0, KeyType.STRING)));

            if (editScript) {
                myersDiff.editScript(firstTable, secondTable, out);
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
