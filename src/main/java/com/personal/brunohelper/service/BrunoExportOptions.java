package com.personal.brunohelper.service;

import com.intellij.psi.PsiClass;
import com.personal.brunohelper.i18n.BrunoHelperBundle;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class BrunoExportOptions {

    private BrunoExportOptions() {
    }

    public static @Nullable String validateBaseOutputDirectory(@Nullable String configuredDirectory, boolean allowBlank) {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            return allowBlank ? null : BrunoHelperBundle.message("export.validation.baseOutput.blank");
        }
        Path outputDirectory;
        try {
            outputDirectory = Paths.get(configuredDirectory.trim()).normalize();
        } catch (InvalidPathException exception) {
            return BrunoHelperBundle.message("export.validation.baseOutput.invalid");
        }
        if (!outputDirectory.isAbsolute()) {
            return BrunoHelperBundle.message("export.validation.baseOutput.absolute");
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
        return resolveControllerDirectory(projectDirectory, controllerName, null);
    }

    public static Path resolveControllerDirectory(Path projectDirectory, String controllerName, @Nullable PsiClass controllerClass) {
        String safeControllerName = sanitizeFileSystemName(controllerName == null || controllerName.isBlank() ? "Controller" : controllerName);
        String subDirectory = extractSubDirectoryFromPackage(controllerClass);
        if (subDirectory != null && !subDirectory.isBlank()) {
            return projectDirectory.resolve(subDirectory).resolve(safeControllerName);
        }
        return projectDirectory.resolve(safeControllerName);
    }

    private static @Nullable String extractSubDirectoryFromPackage(@Nullable PsiClass controllerClass) {
        if (controllerClass == null) {
            return null;
        }
        String packageName = controllerClass.getQualifiedName();
        if (packageName == null) {
            return null;
        }
        int lastDotIndex = packageName.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return null;
        }
        packageName = packageName.substring(0, lastDotIndex);

        String[] parts = packageName.split("\\.");
        String subDirectory = null;
        boolean foundController = false;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("controller")) {
                if (i + 1 < parts.length) {
                    subDirectory = parts[i + 1];
                    foundController = true;
                    break;
                }
            }
        }
        return foundController ? sanitizeFileSystemName(subDirectory) : null;
    }

    public static String deriveCollectionName(String controllerName) {
        String normalized = controllerName.endsWith("Controller")
                ? controllerName.substring(0, controllerName.length() - "Controller".length())
                : controllerName;
        return normalized.isBlank() ? "CollectionExport" : normalized;
    }

    public static String sanitizeFileSystemName(String value) {
        String normalized = value == null ? "" : value.trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]+", "-");
        normalized = normalized.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-|-$", "");
        return normalized.isBlank() ? "collection-export" : normalized;
    }
}
