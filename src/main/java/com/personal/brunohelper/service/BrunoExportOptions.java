package com.personal.brunohelper.service;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class BrunoExportOptions {

    private static final List<String> BRU_EXECUTABLE_NAMES = List.of("bru.cmd", "bru.exe", "bru.bat", "bru");

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

    public static @Nullable String resolveBruCliPath(@Nullable String configuredPath) {
        String normalized = normalizeBruCliPath(configuredPath);
        if (normalized == null) {
            return null;
        }
        Path cliPath = parsePath(normalized);
        if (cliPath == null) {
            return isCommandName(normalized) ? normalized : null;
        }
        if (Files.isDirectory(cliPath)) {
            Path executable = findExecutableInDirectory(cliPath);
            return executable == null ? null : executable.toString();
        }
        return Files.isRegularFile(cliPath) ? cliPath.toString() : (isCommandName(normalized) ? normalized : null);
    }

    public static boolean hasConfiguredBruCliPath(@Nullable String configuredPath) {
        return normalizeBruCliPath(configuredPath) != null;
    }

    public static @Nullable String validateBruCliPath(@Nullable String configuredPath, boolean allowBlank) {
        String normalized = normalizeBruCliPath(configuredPath);
        if (normalized == null) {
            return allowBlank ? null : "请输入 Bruno CLI 命令或可执行文件路径。";
        }

        Path configured = parsePath(normalized);
        if (configured == null) {
            return isCommandName(normalized)
                    ? null
                    : "请输入 Bruno CLI 命令（如 bru）或 Bruno CLI 可执行文件绝对路径。";
        }
        if (!configured.isAbsolute()) {
            return isCommandName(normalized)
                    ? null
                    : "Bruno CLI 可执行文件路径必须使用绝对路径；如已在 PATH 中，可直接填写 bru。";
        }
        if (!Files.exists(configured)) {
            return "Bruno CLI 路径不存在: " + configured;
        }
        if (Files.isRegularFile(configured)) {
            return null;
        }
        if (!Files.isDirectory(configured)) {
            return "Bruno CLI 配置必须是命令名、可执行文件或包含 CLI 的目录。";
        }
        return findExecutableInDirectory(configured) == null
                ? "所选目录中未找到 Bruno CLI，可执行文件名需为 bru、bru.cmd、bru.exe 或 bru.bat。"
                : null;
    }

    public static String deriveCollectionName(String controllerName) {
        String normalized = controllerName.endsWith("Controller")
                ? controllerName.substring(0, controllerName.length() - "Controller".length())
                : controllerName;
        return normalized.isBlank() ? "BrunoExport" : normalized;
    }

    private static @Nullable Path findExecutableInDirectory(Path directory) {
        for (String executableName : BRU_EXECUTABLE_NAMES) {
            Path candidate = directory.resolve(executableName);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static @Nullable String normalizeBruCliPath(@Nullable String configuredPath) {
        if (configuredPath == null) {
            return null;
        }
        String normalized = configuredPath.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private static @Nullable Path parsePath(String configuredPath) {
        try {
            return Paths.get(configuredPath).normalize();
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private static boolean isCommandName(String configuredPath) {
        return !configuredPath.contains("/")
                && !configuredPath.contains("\\")
                && !configuredPath.contains(" ");
    }
}
