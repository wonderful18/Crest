package io.crest.metadata;

import com.baomidou.mybatisplus.annotation.DbType;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Crest metadata database type. MySQL remains the default runtime path.
 */
public enum MetadataDbType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", DbType.MYSQL, "mysql",
            "classpath:db/migration,classpath:db/demo-migration"),
    OB_MYSQL("ob-mysql", "com.oceanbase.jdbc.Driver", DbType.MYSQL, "ob-mysql",
            "classpath:db/migration,classpath:db/demo-migration"),
    OB_ORACLE("ob-oracle", "com.oceanbase.jdbc.Driver", DbType.ORACLE, "ob-oracle",
            "classpath:db/migration-ob-oracle");

    private final String code;
    private final String driverClassName;
    private final DbType mybatisPlusDbType;
    private final String databaseId;
    private final String defaultFlywayLocations;

    MetadataDbType(String code, String driverClassName, DbType mybatisPlusDbType,
                   String databaseId, String defaultFlywayLocations) {
        this.code = code;
        this.driverClassName = driverClassName;
        this.mybatisPlusDbType = mybatisPlusDbType;
        this.databaseId = databaseId;
        this.defaultFlywayLocations = defaultFlywayLocations;
    }

    public String getCode() {
        return code;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public DbType getMybatisPlusDbType() {
        return mybatisPlusDbType;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getDefaultFlywayLocations() {
        return defaultFlywayLocations;
    }

    public boolean isOceanBase() {
        return this == OB_MYSQL || this == OB_ORACLE;
    }

    public boolean isOracleCompatible() {
        return this == OB_ORACLE;
    }

    public static MetadataDbType from(String value) {
        String normalized = value == null ? MYSQL.code : value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(normalized)) {
            return MYSQL;
        }
        for (MetadataDbType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported metadata database type: " + value);
    }
}
