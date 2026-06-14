package io.crest.datasource.security;

import io.crest.exception.CrestException;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Applies the last in-process SQL safety checks before user supplied SQL reaches
 * Calcite or a JDBC connection. This class intentionally performs lexical checks
 * only; datasource-specific authorization, parameter binding, and dialect
 * validation stay in the caller.
 */
public final class SqlSecurityPolicy {

    /**
     * Utility class; all checks are stateless and must not carry request data.
     */
    private SqlSecurityPolicy() {
    }

    /**
     * Allows read-only SQL entry points after the statement has passed the
     * single-statement guard. The accepted prefixes cover query and metadata
     * inspection commands used by datasource preview and field discovery flows.
     *
     * @param sql original SQL text from the request
     * @return the original SQL text so callers keep existing formatting
     */
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
        return sql;
    }

    /**
     * Normalizes and rejects inputs that can change statement boundaries before
     * execution. Semicolons inside string literals, quoted identifiers, or SQL
     * comments remain valid because they do not introduce another executable
     * statement.
     *
     * @param sql original SQL text from the request
     * @return normalized SQL text used by downstream safety checks
     */
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

    /**
     * Detects whether a top-level semicolon is followed by another executable
     * token. A trailing semicolon followed only by whitespace or comments is
     * treated as part of one statement for compatibility with SQL clients.
     */
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

    /**
     * Finds semicolons that are outside quoted text and comments. This is a
     * small statement-boundary scanner, not a full SQL parser, so it only tracks
     * constructs that can hide semicolons in supported datasource queries.
     */
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

    /**
     * Checks the suffix after a semicolon for executable content while ignoring
     * whitespace and comments. Any other token means the input contains another
     * statement and must be rejected.
     */
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

    /**
     * Advances to the end of a line comment so semicolons inside comments cannot
     * be interpreted as statement separators.
     */
    private static int skipLineComment(String sql, int start) {
        int lineEnd = sql.indexOf('\n', start);
        return lineEnd < 0 ? sql.length() : lineEnd;
    }

    /**
     * Advances to the end of a block comment. Unterminated block comments are
     * consumed to the end of the SQL text because they cannot expose a following
     * executable statement within the same input.
     */
    private static int skipBlockComment(String sql, int start) {
        int blockEnd = sql.indexOf("*/", start);
        return blockEnd < 0 ? sql.length() : blockEnd + 1;
    }

    /**
     * Rejects control characters that can obscure log output, query previews, or
     * downstream driver behavior. Standard SQL whitespace stays valid.
     */
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
