package io.crest.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
/**
 * 为 OB Oracle 元数据库中已知的保留字段补充引号，兼容 MyBatis Plus 生成的通用 SQL。
 */
public class ObOracleReservedIdentifierInterceptor implements Interceptor {

    private static final Map<String, List<String>> RESERVED_COLUMNS_BY_TABLE = Map.ofEntries(
            Map.entry("dataset_group", List.of("level", "mode")),
            Map.entry("dataset_field", List.of("size")),
            Map.entry("workspace_recent_resource", List.of("uid", "time")),
            Map.entry("workspace_favorite_resource", List.of("uid", "time")),
            Map.entry("iam_user_org", List.of("uid")),
            Map.entry("iam_user_role", List.of("uid")),
            Map.entry("iam_org", List.of("level")),
            Map.entry("ref_area", List.of("level")),
            Map.entry("bi_visualization", List.of("level")),
            Map.entry("bi_visualization_snapshot", List.of("level")),
            Map.entry("visualization_template", List.of("level")),
            Map.entry("visualization_template_category", List.of("level"))
    );

    private final boolean enabled;

    public ObOracleReservedIdentifierInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (enabled && invocation.getTarget() instanceof StatementHandler statementHandler) {
            BoundSql boundSql = statementHandler.getBoundSql();
            String sql = boundSql.getSql();
            String rewrittenSql = rewrite(sql);
            if (!sql.equals(rewrittenSql)) {
                setSql(statementHandler, rewrittenSql);
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private String rewrite(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        String rewrittenSql = normalizeMysqlSql(rewriteMysqlFunctions(sql));
        String lowerSql = rewrittenSql.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : RESERVED_COLUMNS_BY_TABLE.entrySet()) {
            if (!lowerSql.contains(entry.getKey())) {
                continue;
            }
            for (String column : entry.getValue()) {
                rewrittenSql = quoteIdentifier(rewrittenSql, column);
            }
        }
        return rewrittenSql;
    }

    private String normalizeMysqlSql(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'' || current == '"') {
                int quotedEnd = consumeQuotedSegment(sql, index, current);
                result.append(sql, index, quotedEnd + 1);
                index = quotedEnd;
                continue;
            }
            if (current == '`') {
                int quotedEnd = consumeQuotedSegment(sql, index, current);
                result.append(normalizeIdentifier(sql.substring(index + 1, quotedEnd)));
                index = quotedEnd;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private String normalizeIdentifier(String identifier) {
        String value = identifier.replace("``", "`");
        if (value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"").toUpperCase(Locale.ROOT) + "\"";
    }

    private String rewriteMysqlFunctions(String sql) {
        String rewrittenSql = rewriteOutsideQuotedSegments(sql,
                Pattern.compile("(?i)\\buuid\\s*\\(\\s*\\)"),
                "LOWER(RAWTOHEX(SYS_GUID()))");
        return rewriteOutsideQuotedSegments(rewrittenSql,
                Pattern.compile("(?i)\\bifnull\\s*\\("),
                "NVL(");
    }

    private String quoteIdentifier(String sql, String identifier) {
        Pattern pattern = Pattern.compile("(?i)(?<![\"`])\\b" + Pattern.quote(identifier) + "\\b(?![\"`])");
        String replacement = "\"" + identifier.toUpperCase(Locale.ROOT) + "\"";
        StringBuilder result = new StringBuilder(sql.length());
        int segmentStart = 0;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (!isSqlQuote(current)) {
                continue;
            }
            result.append(quoteSegment(sql.substring(segmentStart, index), pattern, replacement));
            int quotedEnd = consumeQuotedSegment(sql, index, current);
            result.append(sql, index, quotedEnd + 1);
            index = quotedEnd;
            segmentStart = quotedEnd + 1;
        }
        result.append(quoteSegment(sql.substring(segmentStart), pattern, replacement));
        return result.toString();
    }

    private boolean isSqlQuote(char value) {
        return value == '\'' || value == '"' || value == '`';
    }

    private int consumeQuotedSegment(String sql, int start, char quote) {
        for (int index = start + 1; index < sql.length(); index++) {
            if (sql.charAt(index) != quote) {
                continue;
            }
            if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                index++;
                continue;
            }
            return index;
        }
        return sql.length() - 1;
    }

    private String quoteSegment(String sql, Pattern pattern, String replacement) {
        return pattern.matcher(sql).replaceAll(Matcher.quoteReplacement(replacement));
    }

    private String rewriteOutsideQuotedSegments(String sql, Pattern pattern, String replacement) {
        StringBuilder result = new StringBuilder(sql.length());
        int segmentStart = 0;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (!isSqlQuote(current)) {
                continue;
            }
            result.append(quoteSegment(sql.substring(segmentStart, index), pattern, replacement));
            int quotedEnd = consumeQuotedSegment(sql, index, current);
            result.append(sql, index, quotedEnd + 1);
            index = quotedEnd;
            segmentStart = quotedEnd + 1;
        }
        result.append(quoteSegment(sql.substring(segmentStart), pattern, replacement));
        return result.toString();
    }

    private void setSql(StatementHandler statementHandler, String sql) {
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        try {
            metaObject.setValue("delegate.boundSql.sql", sql);
        } catch (ReflectionException ignored) {
            metaObject.setValue("boundSql.sql", sql);
        }
    }
}
