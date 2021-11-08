package ru.study21.jcsv.xxl.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileManager {
    private final Path _pathToDirectory;
    private final List<Path> _files = new ArrayList<>();
    private final List<FileManager> _directories = new ArrayList<>();

    private FileManager(Path pathToDirectory) {
        this._pathToDirectory = pathToDirectory;
    }

    public static FileManager createTempDirectory(String prefix) throws IOException {
        return new FileManager(Files.createTempDirectory(prefix));
    }

    public static FileManager createTempDirectoryWithPath(Path path, String prefix) throws IOException {
        return new FileManager(Files.createTempDirectory(path, prefix));
    }

    public FileManager createTempDirectoryInside(String prefix) throws IOException {
        FileManager internalDirectory = FileManager.createTempDirectoryWithPath(_pathToDirectory, prefix);
        _directories.add(internalDirectory);
        return internalDirectory;
    }

    public Path getPathToDirectory() {
        return _pathToDirectory;
    }

    public List<Path> getFiles() {
        return _files;
    }

    public List<Path> getAllFiles() {
        List<Path> files = new ArrayList<>();
        for (FileManager fileManager : _directories) {
            files.addAll(fileManager.getAllFiles());
        }
        files.addAll(_files);
        return files;
    }

    public List<Path> getDirectories() {
        return _directories.stream()
                .map(FileManager::getPathToDirectory)
                .collect(Collectors.toList());
    }

    public List<Path> getAllDirectories() {
        List<Path> directories = new ArrayList<>();
        for (FileManager fileManager : _directories) {
            directories.addAll(fileManager.getAllDirectories());
        }
        directories.addAll(getDirectories());
        return directories;
    }

    public Path createTempFile(String prefix) throws IOException {
        return createTempFileWithSuffix(prefix, null);
    }

    public Path createTempFileWithSuffix(String prefix, String suffix) throws IOException {
        Path path = Files.createTempFile(_pathToDirectory, prefix, suffix);
        _files.add(path);
        return path;
    }

    public void delete() throws IOException {
        for (FileManager fileManager : _directories) {
            fileManager.delete();
        }
        for (Path file : _files) {
            Files.delete(file);
        }
        Files.delete(_pathToDirectory);
    }
}
