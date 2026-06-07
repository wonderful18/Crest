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
        return sql;
    }

    public static String validateSingleStatement(String sql) {
        if (StringUtils.isBlank(sql)) {
            CrestException.throwException("SQL must not be empty");
        }
        String normalized = Normalizer.normalize(sql, Normalizer.Form.NFKC).trim();
        if (normalized.indexOf('\0') >= 0 || containsControlCharacter(normalized)) {
            CrestException.throwException("SQL contains illegal characters");
        }
        String body = normalized.endsWith(";") ? normalized.substring(0, normalized.length() - 1) : normalized;
        if (body.contains(";")) {
            CrestException.throwException("Multiple SQL statements are not allowed");
        }
        return normalized;
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
