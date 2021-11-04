package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;

import java.io.File;

class Arguments {
    @CommandLine.Parameters(index = "0")
    String command;
    @CommandLine.Parameters(index = "1..*")
    File[] files;

    @CommandLine.Option(names = "-withHeader")
    boolean withHeader;
}
