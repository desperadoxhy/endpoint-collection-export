package com.personal.brunohelper.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FieldBlacklistUtil {

    private FieldBlacklistUtil() {
    }

    public static final List<String> PRESET_BLACKLIST = List.of(
            "id",
            "createTime",
            "updateTime",
            "createBy",
            "updateBy",
            "createdBy",
            "updatedBy",
            "deleted",
            "version",
            "tenantId",
            "delFlag",
            "createTime",
            "updateTime",
            "createUser",
            "updateUser",
            "isDeleted",
            "remark"
    );

    public static boolean isBlacklisted(String fieldName, List<String> patterns) {
        if (fieldName == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        String normalizedName = fieldName.trim();
        if (normalizedName.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            String trimmedPattern = pattern.trim();
            if (trimmedPattern.isEmpty()) {
                continue;
            }
            try {
                if (matchesPattern(normalizedName, trimmedPattern)) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                continue;
            }
        }
        return false;
    }

    private static boolean matchesPattern(String fieldName, String pattern) {
        if (isRegexPattern(pattern)) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            return regex.matcher(fieldName).matches();
        }
        String wildcardRegex = wildcardToRegex(pattern);
        if (!pattern.contains("*")) {
            wildcardRegex += ".*";
        }
        Pattern wildcardPattern = Pattern.compile(wildcardRegex, Pattern.CASE_INSENSITIVE);
        return wildcardPattern.matcher(fieldName).matches();
    }

    private static boolean isRegexPattern(String pattern) {
        if (pattern.startsWith("^") || pattern.endsWith("$")) {
            return true;
        }
        String regexMetaChars = ".+${}[]()|\\";
        for (char c : regexMetaChars.toCharArray()) {
            if (pattern.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < wildcard.length(); i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '|':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '+':
                    regex.append("\\").append(c);
                    break;
                case '\\':
                    regex.append("\\\\");
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.toString();
    }

    public static List<String> validatePatterns(List<String> patterns) {
        List<String> errors = new ArrayList<>();
        if (patterns == null) {
            return errors;
        }
        for (int i = 0; i < patterns.size(); i++) {
            String pattern = patterns.get(i);
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            String trimmed = pattern.trim();
            try {
                if (isRegexPattern(trimmed)) {
                    Pattern.compile(trimmed);
                } else {
                    wildcardToRegex(trimmed);
                }
            } catch (PatternSyntaxException e) {
                errors.add("Pattern at line " + (i + 1) + ": " + e.getMessage());
            }
        }
        return errors;
    }
}
