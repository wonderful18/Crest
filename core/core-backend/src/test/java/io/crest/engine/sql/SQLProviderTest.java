package io.crest.engine.sql;

import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartViewDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLProviderTest {

    @Test
    void createQuerySQLUsesPreviewTemplate() {
        SQLMeta sqlMeta = basicSqlMeta();

        String sql = assertDoesNotThrow(() -> SQLProvider.createQuerySQL(sqlMeta, true, true, false));

        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM"));
        assertTrue(sql.contains("source_table"));
    }

    @Test
    void createChartQuerySQLUsesPreviewTemplate() {
        SQLMeta sqlMeta = basicSqlMeta();
        sqlMeta.setYFields(List.of(SQLObj.builder()
                .fieldName("SUM(amount)")
                .fieldAlias("total_amount")
                .build()));
        ChartViewDTO view = new ChartViewDTO();
        view.setType("bar");
        view.setResultMode("all");

        String sql = assertDoesNotThrow(() -> SQLProvider.createQuerySQL(sqlMeta, true, true, view));

        assertTrue(sql.contains("source_table"));
        assertTrue(sql.contains("total_amount"));
    }

    private SQLMeta basicSqlMeta() {
        SQLMeta sqlMeta = new SQLMeta();
        sqlMeta.setTable(SQLObj.builder()
                .tableName("source_table")
                .tableAlias("t0")
                .build());
        sqlMeta.setXFields(List.of(SQLObj.builder()
                .fieldName("department")
                .fieldAlias("department")
                .build()));
        return sqlMeta;
    }
}
