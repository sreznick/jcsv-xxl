/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ru.study21.jcsv.xxl.app;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        Arguments arguments = CommandLine.populateCommand(new Arguments(), args);

        System.out.println("arguments: " + arguments);
    }
}