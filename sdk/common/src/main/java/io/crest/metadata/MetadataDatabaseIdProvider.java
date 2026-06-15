package io.crest.metadata;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Supplies MyBatis databaseId from Crest metadata database configuration.
 */
public class MetadataDatabaseIdProvider implements DatabaseIdProvider {

    private final Environment environment;

    public MetadataDatabaseIdProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setProperties(Properties p) {
        // Configuration is read from Spring Environment.
    }

    @Override
    public String getDatabaseId(DataSource dataSource) throws SQLException {
        return MetadataDbTypeResolver.resolve(environment).getDatabaseId();
    }
}
