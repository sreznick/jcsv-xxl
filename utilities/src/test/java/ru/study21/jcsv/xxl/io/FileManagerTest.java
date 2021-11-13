package ru.study21.jcsv.xxl.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileManagerTest {
    @Test
    public void testCreateTempDirectory() {
        try {
            FileManager fileManager = FileManager.createTempDirectory("test");
            Path directory = fileManager.getPathToDirectory();

            assertNotNull(directory);
            assertTrue(Files.exists(directory));
            assertTrue(Files.isDirectory(directory));

            fileManager.delete();
            assertTrue(Files.notExists(directory));
        } catch (IOException e) {
            Assertions.fail("Unexpected " + e);
        }
    }

    @Test
    public void testCreateTempDirectoryWithFiles() {
        try {
            FileManager fileManager = FileManager.createTempDirectory("test");
            Path directory = fileManager.getPathToDirectory();
            assertNotNull(directory);
            assertTrue(Files.exists(directory));
            assertTrue(Files.isDirectory(directory));

            Path file1 = fileManager.createTempFile("file1");
            assertNotNull(file1);
            assertTrue(Files.exists(file1));
            assertTrue(Files.isRegularFile(file1));

            Path file2 = fileManager.createTempFile("file2");
            assertNotNull(file2);
            assertTrue(Files.exists(file2));
            assertTrue(Files.isRegularFile(file2));

            Path file3 = fileManager.createTempFileWithSuffix("file3", ".txt");
            assertNotNull(file3);
            assertTrue(Files.exists(file3));
            assertTrue(Files.isRegularFile(file3));

            Path file4 = fileManager.createTempFileWithSuffix("file4", ".txt");
            assertNotNull(file4);
            assertTrue(Files.exists(file4));
            assertTrue(Files.isRegularFile(file4));

            fileManager.delete();
            assertTrue(Files.notExists(file1));
            assertTrue(Files.notExists(file2));
            assertTrue(Files.notExists(file3));
            assertTrue(Files.notExists(file4));
            assertTrue(Files.notExists(directory));
        } catch (IOException e) {
            Assertions.fail("Unexpected " + e);
        }
    }


    @Test
    public void testCreateTempDirectoryWithFoldersInside() {
        try {
            FileManager fileManager = FileManager.createTempDirectory("test");
            Path directory = fileManager.getPathToDirectory();
            assertNotNull(directory);
            assertTrue(Files.exists(directory));
            assertTrue(Files.isDirectory(directory));

            FileManager folder1 = fileManager.createTempDirectoryInside("folder1");
            assertNotNull(folder1);
            assertTrue(Files.exists(folder1.getPathToDirectory()));
            assertTrue(Files.isDirectory(folder1.getPathToDirectory()));

            FileManager folder2 = folder1.createTempDirectoryInside("folder2");
            assertNotNull(folder2);
            assertTrue(Files.exists(folder2.getPathToDirectory()));
            assertTrue(Files.isDirectory(folder2.getPathToDirectory()));

            Path file1 = fileManager.createTempFile("file1");
            assertNotNull(file1);
            assertTrue(Files.exists(file1));
            assertTrue(Files.isRegularFile(file1));

            Path file2 = folder1.createTempFile("file2");
            assertNotNull(file2);
            assertTrue(Files.exists(file2));
            assertTrue(Files.isRegularFile(file2));

            Path file3 = folder2.createTempFile("file3");
            assertNotNull(file3);
            assertTrue(Files.exists(file3));
            assertTrue(Files.isRegularFile(file3));

            List<Path> files = fileManager.getFiles();
            assertEquals(1, files.size());
            assertTrue(List.of(file1).containsAll(files));

            List<Path> allFiles = fileManager.getAllFiles();
            assertEquals(3, allFiles.size());
            assertTrue(List.of(file1, file2, file3).containsAll(allFiles));

            List<Path> allDirectories = fileManager.getAllDirectories();
            assertEquals(2, allDirectories.size());
            assertTrue(List.of(folder1.getPathToDirectory(), folder2.getPathToDirectory()).containsAll(allDirectories));

            fileManager.delete();
            assertTrue(Files.notExists(file3));
            assertTrue(Files.notExists(folder2.getPathToDirectory()));
            assertTrue(Files.notExists(file2));
            assertTrue(Files.notExists(folder1.getPathToDirectory()));
            assertTrue(Files.notExists(file1));
            assertTrue(Files.notExists(directory));
        } catch (IOException e) {
            Assertions.fail("Unexpected " + e);
        }
    }
}
