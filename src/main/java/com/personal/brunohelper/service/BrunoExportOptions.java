package com.personal.brunohelper.service;

import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class BrunoExportOptions {

    private BrunoExportOptions() {
    }

    public static @Nullable String validateBaseOutputDirectory(@Nullable String configuredDirectory, boolean allowBlank) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return allowBlank ? null : "请输入 Bruno 基础输出目录。";
        }
        Path outputDirectory;
        try {
            outputDirectory = Paths.get(configuredDirectory.trim()).normalize();
        } catch (InvalidPathException exception) {
            return "Bruno 基础输出目录格式无效。";
        }
        if (!outputDirectory.isAbsolute()) {
            return "Bruno 基础输出目录必须使用绝对路径。";
        }
        return null;
    }

    public static Path resolveBaseOutputDirectory(String configuredDirectory) {
        return Paths.get(configuredDirectory.trim()).normalize();
    }

    public static Path resolveProjectDirectory(Path baseOutputDirectory, @Nullable String projectName) {
        String safeProjectName = sanitizeFileSystemName(projectName == null || projectName.isBlank() ? "project" : projectName);
        return baseOutputDirectory.resolve(safeProjectName);
    }

    public static Path resolveWorkspaceFile(Path baseOutputDirectory) {
        Path workspaceDirectory = baseOutputDirectory.getParent();
        if (workspaceDirectory == null) {
            workspaceDirectory = baseOutputDirectory;
        }
        return workspaceDirectory.resolve("workspace.yml");
    }

    public static Path resolveControllerDirectory(Path projectDirectory, String controllerName) {
        String safeControllerName = sanitizeFileSystemName(controllerName == null || controllerName.isBlank() ? "Controller" : controllerName);
        return projectDirectory.resolve(safeControllerName);
    }

    public static String deriveCollectionName(String controllerName) {
        String normalized = controllerName.endsWith("Controller")
                ? controllerName.substring(0, controllerName.length() - "Controller".length())
                : controllerName;
        return normalized.isBlank() ? "BrunoExport" : normalized;
    }

    public static String sanitizeFileSystemName(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]+", "-");
        normalized = normalized.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "bruno-export" : normalized;
    }
}
