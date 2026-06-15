package io.crest.metadata;

/**
 * SQL fragments that differ between supported Crest metadata databases.
 */
public interface MetadataDbDialect {

    MetadataDbType type();

    String quoteIdentifier(String identifier);

    String limitOffset(String sql, long limit, long offset);

    String ifNull(String expression, String fallback);

    String concat(String... expressions);

    String currentTimestamp();

    String caseInsensitiveLike(String expression, String parameterExpression);
}
