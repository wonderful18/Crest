package io.crest.datasource.security;

import io.crest.exception.CrestException;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

public final class SqlSecurityPolicy {

    private SqlSecurityPolicy() {
    }

    public static String validateReadQuery(String sql) {
        String normalized = validateSingleStatement(sql);
        String lower = normalized.stripLeading().toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select")
                || lower.startsWith("with")
                || lower.startsWith("show")
                || lower.startsWith("desc")
                || lower.startsWith("describe")
                || lower.startsWith("explain"))) {
            CrestException.throwException("Only read-only SQL is allowed in this context");
        }
        return normalized;
    }

    public static String validateSingleStatement(String sql) {
        if (StringUtils.isBlank(sql)) {
            CrestException.throwException("SQL must not be empty");
        }
        String normalized = Normalizer.normalize(sql, Normalizer.Form.NFKC).trim();
        if (normalized.indexOf('\0') >= 0 || containsControlCharacter(normalized)) {
            CrestException.throwException("SQL contains illegal characters");
        }
        if (containsExecutableSqlAfterTopLevelSemicolon(normalized)) {
            CrestException.throwException("Multiple SQL statements are not allowed");
        }
        return normalized;
    }

    private static boolean containsExecutableSqlAfterTopLevelSemicolon(String sql) {
        int semicolonIndex = findTopLevelSemicolon(sql, 0);
        if (semicolonIndex < 0) {
            return false;
        }
        int nextSemicolonIndex = findTopLevelSemicolon(sql, semicolonIndex + 1);
        if (nextSemicolonIndex >= 0) {
            return true;
        }
        return hasExecutableContent(sql, semicolonIndex + 1);
    }

    private static int findTopLevelSemicolon(String sql, int start) {
        char quote = 0;
        for (int i = start; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
            if (quote != 0) {
                if (current == '\\' && quote != '`') {
                    i++;
                } else if (current == quote) {
                    if (next == quote && quote != '`') {
                        i++;
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (current == '\'' || current == '"' || current == '`') {
                quote = current;
                continue;
            }
            if (current == '-' && next == '-') {
                i = skipLineComment(sql, i + 2);
                continue;
            }
            if (current == '#') {
                i = skipLineComment(sql, i + 1);
                continue;
            }
            if (current == '/' && next == '*') {
                i = skipBlockComment(sql, i + 2);
                continue;
            }
            if (current == ';') {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasExecutableContent(String sql, int start) {
        for (int i = start; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : 0;
            if (Character.isWhitespace(current)) {
                continue;
            }
            if (current == '-' && next == '-') {
                i = skipLineComment(sql, i + 2);
                continue;
            }
            if (current == '#') {
                i = skipLineComment(sql, i + 1);
                continue;
            }
            if (current == '/' && next == '*') {
                i = skipBlockComment(sql, i + 2);
                continue;
            }
            return true;
        }
        return false;
    }

    private static int skipLineComment(String sql, int start) {
        int lineEnd = sql.indexOf('\n', start);
        return lineEnd < 0 ? sql.length() : lineEnd;
    }

    private static int skipBlockComment(String sql, int start) {
        int blockEnd = sql.indexOf("*/", start);
        return blockEnd < 0 ? sql.length() : blockEnd + 1;
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isISOControl(current) && current != '\n' && current != '\r' && current != '\t') {
                return true;
            }
        }
        return false;
    }
}
