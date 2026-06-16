package io.crest.metadata;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 元数据库方言工厂及内置实现。
 */
public final class MetadataDbDialects {

    private static final MetadataDbDialect MYSQL = new MysqlLikeDialect(MetadataDbType.MYSQL);
    private static final MetadataDbDialect OB_MYSQL = new MysqlLikeDialect(MetadataDbType.OB_MYSQL);
    private static final MetadataDbDialect OB_ORACLE = new ObOracleDialect();

    private MetadataDbDialects() {
    }

    public static MetadataDbDialect current(Environment environment) {
        return forType(MetadataDbTypeResolver.resolve(environment));
    }

    public static MetadataDbDialect forType(MetadataDbType type) {
        return switch (type) {
            case MYSQL -> MYSQL;
            case OB_MYSQL -> OB_MYSQL;
            case OB_ORACLE -> OB_ORACLE;
        };
    }

    private record MysqlLikeDialect(MetadataDbType type) implements MetadataDbDialect {
        @Override
        public String quoteIdentifier(String identifier) {
            return "`" + StringUtils.defaultString(identifier).replace("`", "``") + "`";
        }

        @Override
        public String limitOffset(String sql, long limit, long offset) {
            return sql + " LIMIT " + limit + " OFFSET " + offset;
        }

        @Override
        public String ifNull(String expression, String fallback) {
            return "IFNULL(" + expression + ", " + fallback + ")";
        }

        @Override
        public String concat(String... expressions) {
            return "CONCAT(" + String.join(", ", expressions) + ")";
        }

        @Override
        public String currentTimestamp() {
            return "NOW()";
        }

        @Override
        public String caseInsensitiveLike(String expression, String parameterExpression) {
            return "LOWER(" + expression + ") LIKE LOWER(CONCAT('%', " + parameterExpression + ", '%'))";
        }

        @Override
        public String stringLiteral(String value) {
            return collate("'" + StringUtils.defaultString(value).replace("'", "''") + "'");
        }

        @Override
        public String stringCast(String expression) {
            return collate("CAST(" + expression + " AS CHAR)");
        }

        @Override
        public String nullableString() {
            return collate("CAST(NULL AS CHAR)");
        }

        @Override
        public String collate(String expression) {
            return expression + " COLLATE utf8mb4_unicode_ci";
        }

        @Override
        public String numberCast(String expression) {
            return "CAST(" + expression + " AS UNSIGNED)";
        }

        @Override
        public String insertIgnore(String table, String columns, String values) {
            return "INSERT IGNORE INTO " + table + " (" + columns + ") VALUES (" + values + ")";
        }

        @Override
        public String mergeIfMissing(String table, String columns, String values, String conflictPredicate) {
            return insertIgnore(table, columns, values);
        }

        @Override
        public String upsert(String table, String columns, String values, String conflictPredicate,
                             List<String> updateAssignments) {
            return "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ") ON DUPLICATE KEY UPDATE "
                    + String.join(", ", updateAssignments);
        }
    }

    private static final class ObOracleDialect implements MetadataDbDialect {
        @Override
        public MetadataDbType type() {
            return MetadataDbType.OB_ORACLE;
        }

        @Override
        public String quoteIdentifier(String identifier) {
            return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
        }

        @Override
        public String limitOffset(String sql, long limit, long offset) {
            return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }

        @Override
        public String ifNull(String expression, String fallback) {
            return "NVL(" + expression + ", " + fallback + ")";
        }

        @Override
        public String concat(String... expressions) {
            return Arrays.stream(expressions).collect(Collectors.joining(" || "));
        }

        @Override
        public String currentTimestamp() {
            return "SYSTIMESTAMP";
        }

        @Override
        public String caseInsensitiveLike(String expression, String parameterExpression) {
            return "LOWER(" + expression + ") LIKE LOWER('%' || " + parameterExpression + " || '%')";
        }

        @Override
        public String stringLiteral(String value) {
            return "'" + StringUtils.defaultString(value).replace("'", "''") + "'";
        }

        @Override
        public String stringCast(String expression) {
            return "TO_CHAR(" + expression + ")";
        }

        @Override
        public String nullableString() {
            return "CAST(NULL AS VARCHAR2(255))";
        }

        @Override
        public String collate(String expression) {
            return expression;
        }

        @Override
        public String numberCast(String expression) {
            return "TO_NUMBER(" + expression + ")";
        }

        @Override
        public String insertIgnore(String table, String columns, String values) {
            return mergeIfMissing(table, columns, values, null);
        }

        @Override
        public String mergeIfMissing(String table, String columns, String values, String conflictPredicate) {
            if (StringUtils.isBlank(conflictPredicate)) {
                throw new IllegalArgumentException("OB Oracle merge requires a conflict predicate");
            }
            return "INSERT INTO " + table + " (" + columns + ") "
                    + "SELECT " + values + " FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM " + table
                    + " WHERE " + conflictPredicate + ")";
        }

        @Override
        public String upsert(String table, String columns, String values, String conflictPredicate,
                             List<String> updateAssignments) {
            if (StringUtils.isBlank(conflictPredicate)) {
                throw new IllegalArgumentException("OB Oracle upsert requires a conflict predicate");
            }
            String updateSql = updateAssignments == null || updateAssignments.isEmpty()
                    ? ""
                    : " UPDATE SET " + String.join(", ", updateAssignments);
            return "MERGE INTO " + table + " target USING (SELECT " + values + " FROM DUAL) source ON ("
                    + conflictPredicate + ") WHEN MATCHED THEN" + updateSql
                    + " WHEN NOT MATCHED THEN INSERT (" + columns + ") VALUES (" + values + ")";
        }
    }
}
