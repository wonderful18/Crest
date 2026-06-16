package io.crest.metadata;

import com.baomidou.mybatisplus.annotation.DbType;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Crest 元数据库类型，MySQL 始终保持默认运行路径。
 */
public enum MetadataDbType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", DbType.MYSQL, "mysql",
            "classpath:db/migration,classpath:db/demo-migration"),
    OB_MYSQL("ob-mysql", "com.oceanbase.jdbc.Driver", DbType.MYSQL, "ob-mysql",
            "classpath:db/migration-ob-mysql"),
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

    // 对外暴露的配置编码，直接对应 CREST_DB_TYPE
    public String getCode() {
        return code;
    }

    // Spring 数据源默认驱动类，由环境后处理器写入占位配置
    public String getDriverClassName() {
        return driverClassName;
    }

    // MyBatis Plus 分页和 SQL 片段所需的数据库枚举
    public DbType getMybatisPlusDbType() {
        return mybatisPlusDbType;
    }

    // MyBatis databaseId，用于按元数据库类型选择 mapper 语句
    public String getDatabaseId() {
        return databaseId;
    }

    // 默认 Flyway 目录，安装脚本和本地启动共用同一份约定
    public String getDefaultFlywayLocations() {
        return defaultFlywayLocations;
    }

    // MySQL 可按需移除演示迁移，OceanBase 目录不加载 MySQL 演示数据
    public String getDefaultFlywayLocations(boolean demoEnabled) {
        if (demoEnabled) {
            return defaultFlywayLocations;
        }
        return defaultFlywayLocations.replace(",classpath:db/demo-migration", "");
    }

    // OceanBase 元库按外部企业数据库处理，启动默认不主动迁移
    public boolean isOceanBase() {
        return this == OB_MYSQL || this == OB_ORACLE;
    }

    // MySQL 保持自动初始化，OceanBase 默认交由离线 SQL 或 DBA 初始化
    public boolean isFlywayEnabledByDefault() {
        return !isOceanBase();
    }

    // Oracle 兼容路径需要额外启用标识符引用和主键生成适配
    public boolean isOracleCompatible() {
        return this == OB_ORACLE;
    }

    // 解析配置值时兼容下划线写法，并对未知类型尽早失败
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
