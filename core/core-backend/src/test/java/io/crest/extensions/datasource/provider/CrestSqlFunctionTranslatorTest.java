package io.crest.extensions.datasource.provider;

import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrestSqlFunctionTranslatorTest {

    @Test
    @DisplayName("MySQL 兼容数据源翻译 Crest 内置日期函数")
    void translateShouldRewriteCrestDateFunctionsForMysql() {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType("mysql");
        DatasourceSchemaDTO obMysql = new DatasourceSchemaDTO();
        obMysql.setType("obMysql");

        String sql = "select CREST_DATE_FORMAT(CREST_STR_TO_DATE(`orders`.`day`, 'yyyy-MM-dd'), 'yyyy-MM') "
                + "from `orders` "
                + "where CREST_UNIX_TIMESTAMP(CREST_FROM_UNIXTIME(`orders`.`ts`, 'yyyy-MM-dd HH:mm:ss')) > 0";

        String translated = CrestSqlFunctionTranslator.translate(sql, datasource);

        assertThat(translated).contains("DATE_FORMAT(STR_TO_DATE(`orders`.`day`, '%Y-%m-%d'), '%Y-%m')");
        assertThat(translated).contains("UNIX_TIMESTAMP(FROM_UNIXTIME(`orders`.`ts`, '%Y-%m-%d %H:%i:%s'))");
        assertThat(translated).doesNotContain("CREST_");
        assertThat(translated).doesNotContain("DE_");
        assertThat(CrestSqlFunctionTranslator.translate(sql, obMysql)).isEqualTo(translated);
    }

    @Test
    @DisplayName("非 MySQL 兼容数据源保持 SQL 不变")
    void translateShouldSkipNonMysqlFamilyDatasource() {
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType("pg");
        String sql = "select CREST_DATE_FORMAT(created_at, 'yyyy-MM-dd') from orders";

        assertThat(CrestSqlFunctionTranslator.translate(sql, datasource)).isEqualTo(sql);
    }

    @Test
    @DisplayName("Oracle 兼容数据源翻译 Crest 内置日期函数")
    void translateShouldRewriteCrestDateFunctionsForOracleLikeDatasource() {
        DatasourceSchemaDTO oracle = new DatasourceSchemaDTO();
        oracle.setType("oracle");
        DatasourceSchemaDTO obOracle = new DatasourceSchemaDTO();
        obOracle.setType("obOracle");

        String sql = "select CREST_DATE_FORMAT(CREST_STR_TO_DATE(\"orders\".\"day\", 'yyyy-MM-dd'), 'yyyy-MM'), "
                + "CREST_DATE_FORMAT(\"orders\".\"created_at\", '%Y-%m-%d %H:%i:%s'), "
                + "CREST_FROM_UNIXTIME(\"orders\".\"ts\", 'yyyy-MM-dd HH:mm:ss') "
                + "from \"orders\" "
                + "where CREST_UNIX_TIMESTAMP(\"orders\".\"created_at\") > 0";

        String translated = CrestSqlFunctionTranslator.translate(sql, oracle);

        assertThat(translated).contains("TO_CHAR(TO_DATE(\"orders\".\"day\", 'YYYY-MM-DD'), 'YYYY-MM')");
        assertThat(translated).contains("TO_CHAR(\"orders\".\"created_at\", 'YYYY-MM-DD HH24:MI:SS')");
        assertThat(translated).contains("TO_CHAR(TIMESTAMP '1970-01-01 00:00:00' + NUMTODSINTERVAL(\"orders\".\"ts\", 'SECOND'), 'YYYY-MM-DD HH24:MI:SS')");
        assertThat(translated).contains("CAST(\"orders\".\"created_at\" AS DATE) - DATE '1970-01-01'");
        assertThat(translated).doesNotContain("CREST_");
        assertThat(CrestSqlFunctionTranslator.translate(sql, obOracle)).isEqualTo(translated);
    }
}
