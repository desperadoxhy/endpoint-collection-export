package com.personal.brunohelper.service;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BrunoExportOptions {

    private static final String BRU_COMMAND = "bru";

    private BrunoExportOptions() {
    }

    public static Path resolveOutputDirectory(@Nullable String projectBasePath, @Nullable String configuredDirectory) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return projectBasePath == null || projectBasePath.isBlank()
                    ? Paths.get("bruno")
                    : Paths.get(projectBasePath, "bruno");
        }
        Path outputDirectory = Paths.get(configuredDirectory);
        if (outputDirectory.isAbsolute()) {
            return outputDirectory;
        }
        return projectBasePath == null || projectBasePath.isBlank()
                ? outputDirectory
                : Paths.get(projectBasePath).resolve(outputDirectory).normalize();
    }

    public static String deriveCollectionName(String controllerName) {
        String normalized = controllerName.endsWith("Controller")
                ? controllerName.substring(0, controllerName.length() - "Controller".length())
                : controllerName;
        return normalized.isBlank() ? "BrunoExport" : normalized;
    }

    public static String resolveBruCommand() {
        if (!isWindows()) {
            return BRU_COMMAND;
        }

        String resolved = resolveCommandOnWindows(BRU_COMMAND, System.getenv("PATH"), System.getenv("PATHEXT"));
        return resolved == null ? BRU_COMMAND : resolved;
    }

    static @Nullable String resolveCommandOnWindows(String commandName, @Nullable String pathEnv, @Nullable String pathExtEnv) {
        if (commandName.isBlank() || commandName.contains("/") || commandName.contains("\\") || commandName.contains(" ")) {
            return null;
        }

        List<String> executableNames = new ArrayList<>();
        String lowerCaseCommandName = commandName.toLowerCase(Locale.ROOT);
        for (String extension : splitPathExtensions(pathExtEnv)) {
            if (!lowerCaseCommandName.endsWith(extension.toLowerCase(Locale.ROOT))) {
                executableNames.add(commandName + extension);
            }
        }
        executableNames.add(commandName);

        for (String pathEntry : splitPathEntries(pathEnv)) {
            Path directory = parsePath(pathEntry);
            if (directory == null) {
                continue;
            }
            for (String executableName : executableNames) {
                String resolved = resolveRegularFileIgnoringCase(directory, executableName);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static List<String> splitPathEntries(@Nullable String pathEnv) {
        if (pathEnv == null || pathEnv.isBlank()) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        for (String entry : pathEnv.split(";")) {
            if (!entry.isBlank()) {
                entries.add(entry.trim());
            }
        }
        return entries;
    }

    private static List<String> splitPathExtensions(@Nullable String pathExtEnv) {
        if (pathExtEnv == null || pathExtEnv.isBlank()) {
            return List.of(".com", ".exe", ".bat", ".cmd");
        }
        List<String> extensions = new ArrayList<>();
        for (String extension : pathExtEnv.split(";")) {
            String normalized = extension.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (!normalized.startsWith(".")) {
                normalized = "." + normalized;
            }
            extensions.add(normalized.toLowerCase(Locale.ROOT));
        }
        return extensions.isEmpty() ? List.of(".com", ".exe", ".bat", ".cmd") : extensions;
    }

    private static @Nullable Path parsePath(String value) {
        try {
            return Paths.get(value).normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static @Nullable String resolveRegularFileIgnoringCase(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (Files.isRegularFile(candidate)) {
            return candidate.toString();
        }
        try (var children = Files.list(directory)) {
            String expectedName = fileName.toLowerCase(Locale.ROOT);
            return children
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).equals(expectedName))
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
