package io.crest.metadata;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 解析当前配置的 Crest 元数据库类型。
 */
public final class MetadataDbTypeResolver {

    public static final String PROPERTY_NAME = "crest.metadata-db.type";
    public static final String ENV_NAME = "CREST_DB_TYPE";

    private MetadataDbTypeResolver() {
    }

    public static MetadataDbType resolve(Environment environment) {
        if (environment == null) {
            return MetadataDbType.MYSQL;
        }
        String value = environment.getProperty(PROPERTY_NAME);
        if (StringUtils.isBlank(value)) {
            value = environment.getProperty(ENV_NAME);
        }
        return MetadataDbType.from(value);
    }
}
