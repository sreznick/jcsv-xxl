package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;
import static picocli.CommandLine.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

@Command(
        name = "jcsvxxl",
        subcommands = {
                SummaryCommand.class
        })
public class JCSVXXLApp {

    @Spec
    Model.CommandSpec spec;

    @Parameters(arity = "1..*")
    List<File> files;

    @Option(names = {"-h", "--withHeader"})
    boolean withHeader = false;

    @Option(names = {"-s", "--separator"})
    char separator = ',';

    public static void main(String[] args) {
        System.exit(new CommandLine(new JCSVXXLApp()).execute(args));
    }

}
