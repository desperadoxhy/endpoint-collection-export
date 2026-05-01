package com.personal.brunohelper.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldBlacklistUtilTest {

    @Test
    void testExactMatch() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("createTime")));
        assertFalse(FieldBlacklistUtil.isBlacklisted("createTime", List.of("updateTime")));
    }

    @Test
    void testWildcardMatch() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("*Time")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("updateTime", List.of("*Time")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("create*")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTimestamp", List.of("create*")));
        assertFalse(FieldBlacklistUtil.isBlacklisted("deleteTime", List.of("create*")));
    }

    @Test
    void testRegexMatch() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("^create.*")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime123", List.of("^create.*\\d+$")));
        assertFalse(FieldBlacklistUtil.isBlacklisted("updateTime", List.of("^create.*")));
    }

    @Test
    void testCaseInsensitiveMatch() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("CreateTime", List.of("createTime")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("CREATETIME", List.of("createTime")));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("CREATETIME")));
    }

    @Test
    void testMultiplePatterns() {
        List<String> patterns = List.of("*Time", "secret", "^internal.*");
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", patterns));
        assertTrue(FieldBlacklistUtil.isBlacklisted("updateTime", patterns));
        assertTrue(FieldBlacklistUtil.isBlacklisted("secretKey", patterns));
        assertTrue(FieldBlacklistUtil.isBlacklisted("internalField", patterns));
        assertFalse(FieldBlacklistUtil.isBlacklisted("publicField", patterns));
    }

    @Test
    void testEmptyPatterns() {
        assertFalse(FieldBlacklistUtil.isBlacklisted("createTime", List.of()));
        assertFalse(FieldBlacklistUtil.isBlacklisted("createTime", null));
    }

    @Test
    void testEmptyFieldName() {
        assertFalse(FieldBlacklistUtil.isBlacklisted("", List.of("createTime")));
        assertFalse(FieldBlacklistUtil.isBlacklisted(null, List.of("createTime")));
    }

    @Test
    void testWhitespacePatterns() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", List.of("  createTime  ")));
        assertFalse(FieldBlacklistUtil.isBlacklisted("createTime", List.of("   ")));
        assertFalse(FieldBlacklistUtil.isBlacklisted("createTime", List.of("", "  ")));
    }

    @Test
    void testWildcardToRegex() {
        assertEquals(".*", FieldBlacklistUtil.wildcardToRegex("*"));
        assertEquals("create.*", FieldBlacklistUtil.wildcardToRegex("create*"));
        assertEquals(".*time", FieldBlacklistUtil.wildcardToRegex("*time"));
        assertEquals("create.*time", FieldBlacklistUtil.wildcardToRegex("create*time"));
        assertEquals(".", FieldBlacklistUtil.wildcardToRegex("?"));
        assertEquals("create.", FieldBlacklistUtil.wildcardToRegex("create?"));
    }

    @Test
    void testPresetBlacklist() {
        assertTrue(FieldBlacklistUtil.isBlacklisted("id", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createTime", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("updateTime", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("createdBy", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("deleted", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("version", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertTrue(FieldBlacklistUtil.isBlacklisted("tenantId", FieldBlacklistUtil.PRESET_BLACKLIST));
        assertFalse(FieldBlacklistUtil.isBlacklisted("username", FieldBlacklistUtil.PRESET_BLACKLIST));
    }

    @Test
    void testValidatePatterns() {
        assertEquals(0, FieldBlacklistUtil.validatePatterns(List.of("createTime", "*Time", "^create.*")).size());
        assertEquals(1, FieldBlacklistUtil.validatePatterns(List.of("valid", "[invalid")).size());
        assertTrue(FieldBlacklistUtil.validatePatterns(List.of("[invalid")).get(0).contains("line 1"));
    }
}
