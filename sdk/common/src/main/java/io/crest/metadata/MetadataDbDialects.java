package io.crest.metadata;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Factory and built-in implementations for metadata database dialects.
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
    }
}
