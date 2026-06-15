package io.crest.metadata;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 根据 Crest 元数据库配置向 MyBatis 提供 databaseId。
 */
public class MetadataDatabaseIdProvider implements DatabaseIdProvider {

    private final Environment environment;

    public MetadataDatabaseIdProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setProperties(Properties p) {
        // 配置统一从 Spring Environment 读取。
    }

    @Override
    public String getDatabaseId(DataSource dataSource) throws SQLException {
        return MetadataDbTypeResolver.resolve(environment).getDatabaseId();
    }
}
