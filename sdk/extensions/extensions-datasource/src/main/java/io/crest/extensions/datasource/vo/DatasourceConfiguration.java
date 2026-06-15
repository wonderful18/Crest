package io.crest.extensions.datasource.vo;

import lombok.Data;

import java.util.List;

@Data
/**
 * Datasource-level configuration for unsupported parameters and metadata SQL.
 */
public class DatasourceConfiguration extends Configuration {
    /**
     * Parameter names that should be rejected for this datasource type.
     */
    private List<String> illegalParameters;
    /**
     * SQL templates used to list tables for this datasource type.
     */
    private List<String> showTableSqls;


    /**
     * Built-in datasource type metadata used by driver and quoting logic.
     */
    static public enum DatasourceType {
        folder("folder", "folder", "folder", null, null, 25),
        API("API", "API", "API", "`", "`", 15),
        Excel("Excel", "Excel", "LOCALFILE", "`", "`", 16),
        ExcelRemote("ExcelRemote", "ExcelRemote", "LOCALFILE", "`", "`", 29),
        mysql("mysql", "Mysql", "OLTP", "`", "`", 27),
        impala("impala", "Apache Impala", "OLAP", "`", "`", 5),
        mariadb("mariadb", "Mariadb", "OLTP", "`", "`", 6),
        StarRocks("StarRocks", "StarRocks", "OLAP", "`", "`", 7),
        es("es", "Elasticsearch", "OLAP", "\"", "\"", 14),
        doris("doris", "Apache Doris", "OLAP", "`", "`", 26),
        TiDB("TiDB", "TiDB", "OLTP", "`", "`", 3),
        oracle("oracle", "ORACLE", "OLTP", "\"", "\"", 1),
        obOracle("obOracle", "OceanBase Oracle", "OLTP", "\"", "\"", 31),
        pg("pg", "PostgreSQL", "OLTP", "\"", "\"", 9),
        redshift("redshift", "AWS Redshift", "OLTP", "\"", "\"", 13),
        db2("db2", "Db2", "OLTP", "", "", 12),
        ck("ck", "Clickhouse", "OLAP", "`", "`", 11),
        h2("h2", "H2", "OLAP", "\"", "\"", 30),
        sqlServer("sqlServer", "Sqlserver", "DL", "[", "]", 2),
        mongo("mongo", "MongoDB", "DL", "`", "`", 10);

        /**
         * Internal datasource type code.
         */
        private String type;
        /**
         * Display name shown in datasource selection.
         */
        private String name;
        /**
         * Stable ordering flag used by datasource lists.
         */
        private Integer flag;
        /**
         * Datasource catalog grouping.
         */
        private String catalog;
        /**
         * Identifier quote prefix.
         */
        private String prefix;
        /**
         * Identifier quote suffix.
         */
        private String suffix;

        /**
         * Creates a datasource type metadata entry.
         */
        DatasourceType(String type, String name, String catalog, String prefix, String suffix, Integer flag) {
            this.type = type;
            this.name = name;
            this.catalog = catalog;
            this.prefix = prefix;
            this.suffix = suffix;
            this.flag = flag;
        }

        /**
         * Returns the internal datasource type code.
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the datasource display name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the datasource catalog grouping.
         */
        public String getCatalog() {
            return catalog;
        }

        /**
         * Returns the identifier quote prefix.
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Returns the identifier quote suffix.
         */
        public String getSuffix() {
            return suffix;
        }

        /**
         * Returns the stable ordering flag.
         */
        public Integer getFlag() {
            return flag;
        }
    }
}
