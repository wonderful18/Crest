package io.crest.engine.trans;

import io.crest.constant.FieldTypeConstants;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartExtFilterDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.ChartViewFieldFilterDTO;
import io.crest.extensions.view.filter.FilterTreeItem;
import io.crest.extensions.view.filter.FilterTreeObj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Quota2SQLObjTest {

    @Test
    void yAxisTextFilterEscapesLiteralValues() {
        SQLMeta meta = meta();
        ChartViewFieldDTO field = field(FieldTypeConstants.STRING, "count");
        field.setFilter(List.of(filter("eq", "O'Brien")));

        Quota2SQLObj.quota2sqlObj(meta, List.of(field), List.of(), false, null, null, null, null);

        assertEquals("(f_ay_0  = 'O''Brien')", meta.getYWheres().get(0));
    }

    @Test
    void yAxisNumericFilterRejectsNonNumericValuesBeforeSqlExecution() {
        SQLMeta meta = meta();
        ChartViewFieldDTO field = field(FieldTypeConstants.INTEGER, "sum");
        field.setFilter(List.of(filter("eq", "u")));

        assertThrows(CrestException.class,
                () -> Quota2SQLObj.quota2sqlObj(meta, List.of(field), List.of(), false, null, null, null, null));
    }

    @Test
    void customWhereNumericFilterRejectsNonNumericValuesBeforeSqlExecution() {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(FieldTypeConstants.INTEGER);
        FilterTreeItem item = new FilterTreeItem();
        item.setType("item");
        item.setField(field);
        item.setTerm("eq");
        item.setValue("u");

        FilterTreeObj tree = new FilterTreeObj();
        tree.setLogic("AND");
        tree.setItems(List.of(item));

        assertThrows(CrestException.class,
                () -> CustomWhere2Str.customWhere2sqlObj(meta, tree, List.of(field), false, null, null, null, null));
    }

    @Test
    void extWhereNumericFilterRejectsNonNumericValuesBeforeSqlExecution() {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(FieldTypeConstants.INTEGER);
        ChartExtFilterDTO filter = new ChartExtFilterDTO();
        filter.setDatasetTableField(field);
        filter.setOperator("eq");
        filter.setValue(List.of("u"));

        assertThrows(CrestException.class,
                () -> ExtWhere2Str.extWhere2sqlOjb(meta, List.of(filter), List.of(field), false, null, null, null, null));
    }

    @Test
    void yAxisNumericInFilterKeepsOnlyNumberLiterals() {
        SQLMeta meta = meta();
        ChartViewFieldDTO field = field(FieldTypeConstants.FLOAT, "sum");
        field.setFilter(List.of(filter("in", "1, 2.5")));

        Quota2SQLObj.quota2sqlObj(meta, List.of(field), List.of(), false, null, null, null, null);

        assertEquals("(f_ay_0  IN (1,2.5))", meta.getYWheres().get(0));
    }

    private SQLMeta meta() {
        SQLMeta meta = new SQLMeta();
        meta.setTable(SQLObj.builder().tableAlias("t_a_0").build());
        return meta;
    }

    private ChartViewFieldDTO field(Integer fieldType, String summary) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(1L);
        field.setOriginName("amount");
        field.setEngineFieldName("f_amount");
        field.setFieldType(fieldType);
        field.setSummary(summary);
        return field;
    }

    private DatasetTableFieldDTO datasetField(Integer fieldType) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(1L);
        field.setOriginName("amount");
        field.setEngineFieldName("f_amount");
        field.setFieldType(fieldType);
        field.setExtractedFieldType(0);
        field.setType("DECIMAL");
        return field;
    }

    private ChartViewFieldFilterDTO filter(String term, String value) {
        ChartViewFieldFilterDTO filter = new ChartViewFieldFilterDTO();
        filter.setTerm(term);
        filter.setValue(value);
        return filter;
    }
}
