package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;

import static picocli.CommandLine.*;


@Command(
        name = "jcsvxxl",
        subcommands = {
                SummaryCommand.class,
                SortCommand.class,
                DiffCommand.class
        })
public class JCSVXXLApp {

    @Spec
    Model.CommandSpec spec;

    @Option(names = {"-h", "--withHeader"})
    boolean withHeader = false;

    @Option(names = {"-s", "--separator"})
    char separator = ',';

    public static void main(String[] args) {
        System.exit(new CommandLine(new JCSVXXLApp()).execute(args));
    }

}
