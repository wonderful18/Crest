package io.crest.dataset.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨源 SQL 处理回归测试。
 * <p>
 * 覆盖本次修复的两个关键点：
 * 1) {@link SqlUtils#convertSingleQuotedAliases} —— Lex.JAVA 下 AS '别名'/AS "别名" 转反引号；
 * 2) {@link SqlUtils#addSchema} 渲染后清除 Calcite 给非 ASCII 字符串加的 _UTF-8 前缀，
 *    以及 LIMIT N 的还原。这两点是跨源带中文 CASE WHEN 别名查询失败（_UTF-8 前缀导致 MySQL 报错）的根因。
 */
class SqlUtilsTest {

    @Test
    void convertSingleQuotedAliasToBacktick() {
        assertEquals("SELECT d1 AS `中文列` FROM t", SqlUtils.convertSingleQuotedAliases("SELECT d1 AS '中文列' FROM t"));
    }

    @Test
    void convertDoubleQuotedAliasToBacktick() {
        assertEquals("SELECT d1 AS `中文列` FROM t", SqlUtils.convertSingleQuotedAliases("SELECT d1 AS \"中文列\" FROM t"));
    }

    @Test
    void convertAliasIsCaseInsensitive() {
        assertEquals("SELECT d1 AS `x` FROM t", SqlUtils.convertSingleQuotedAliases("SELECT d1 as 'x' FROM t"));
    }

    @Test
    void leaveStringLiteralsUntouched() {
        // WHERE 里的字符串字面量不应被误转
        assertEquals("SELECT * FROM t WHERE name = '中文'", SqlUtils.convertSingleQuotedAliases("SELECT * FROM t WHERE name = '中文'"));
    }

    @Test
    void addSchemaRemovesUtf8PrefixFromChineseStringLiterals() {
        // 用户实际场景：跨源 CASE WHEN 中文别名 + 中文字面量
        String sql = "select CASE d1 WHEN 's' THEN '中文' ELSE '其他' END as 中文列 from defect_merge_test";
        String rendered = SqlUtils.addSchema(sql, "s_a_910001");

        // 不应残留 _UTF-8' 前缀
        assertFalse(rendered.contains("_UTF-8'"), "rendered sql should not contain _UTF-8' prefix: " + rendered);
        // 中文字符串字面量应原样保留
        assertTrue(rendered.contains("'中文'"), "rendered sql should keep '中文' literal: " + rendered);
        assertTrue(rendered.contains("'其他'"), "rendered sql should keep '其他' literal: " + rendered);
    }

    @Test
    void addSchemaRestoresLimitInsteadOfFetchNext() {
        String sql = "select * from t LIMIT 100";
        String rendered = SqlUtils.addSchema(sql, "s_a_910001");

        assertFalse(rendered.contains("FETCH NEXT"), "LIMIT should not be converted to FETCH NEXT: " + rendered);
        assertTrue(rendered.contains("LIMIT 100"), "LIMIT 100 should be preserved: " + rendered);
    }

    @Test
    void addSchemaPrependsSchemaToTable() {
        String rendered = SqlUtils.addSchema("select * from defect_merge_test", "s_a_910001");
        assertTrue(rendered.contains("s_a_910001.defect_merge_test") || rendered.contains("`s_a_910001`.`defect_merge_test`"),
                "schema alias should be prepended to table: " + rendered);
    }
}
