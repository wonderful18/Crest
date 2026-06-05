package io.crest.engine.trans;

import io.crest.constant.FieldTypeConstants;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.ChartViewFieldFilterDTO;
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

    private ChartViewFieldFilterDTO filter(String term, String value) {
        ChartViewFieldFilterDTO filter = new ChartViewFieldFilterDTO();
        filter.setTerm(term);
        filter.setValue(value);
        return filter;
    }
}
