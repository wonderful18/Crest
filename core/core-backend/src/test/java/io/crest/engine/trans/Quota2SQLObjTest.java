package io.crest.engine.trans;

import io.crest.api.permissions.dataset.dto.DataSetRowPermissionsTreeDTO;
import io.crest.constant.FieldTypeConstants;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartExtFilterDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.ChartViewFieldFilterDTO;
import io.crest.extensions.view.dto.DatasetRowPermissionsTreeItem;
import io.crest.extensions.view.dto.DatasetRowPermissionsTreeObj;
import io.crest.extensions.view.filter.FilterTreeItem;
import io.crest.extensions.view.filter.FilterTreeObj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void customWhereNormalizesTextPlaceholdersBeforeConvertedDateFilter() {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(FieldTypeConstants.TIME, FieldTypeConstants.STRING, "TEXT", "yyyy/MM/dd");
        FilterTreeItem item = new FilterTreeItem();
        item.setType("item");
        item.setField(field);
        item.setTerm("eq");
        item.setValue("2024-05-27");
        FilterTreeObj tree = new FilterTreeObj();
        tree.setLogic("AND");
        tree.setItems(List.of(item));

        CustomWhere2Str.customWhere2sqlObj(meta, tree, List.of(field), false, null, null, null, null);

        assertContainsNormalizedTextOrigin(meta.getCustomWheres(), "t_a_0.`f_amount`");
        assertTrue(meta.getCustomWheres().contains("DE_STR_TO_DATE(CASE WHEN t_a_0.`f_amount` IS NULL THEN NULL"), meta.getCustomWheres());
    }

    @Test
    void extWhereNormalizesTextPlaceholdersBeforeConvertedMetricFilter() {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(FieldTypeConstants.FLOAT, FieldTypeConstants.STRING, "TEXT", null);
        ChartExtFilterDTO filter = new ChartExtFilterDTO();
        filter.setDatasetTableField(field);
        filter.setOperator("gt");
        filter.setValue(List.of("10"));

        ExtWhere2Str.extWhere2sqlOjb(meta, List.of(filter), List.of(field), false, null, null, null, null);

        assertContainsNormalizedTextOrigin(meta.getExtWheres(), "t_a_0.`f_amount`");
        assertTrue(meta.getExtWheres().contains("CAST(CASE WHEN t_a_0.`f_amount` IS NULL THEN NULL"), meta.getExtWheres());
    }

    @Test
    void rowPermissionTreeNormalizesTextPlaceholdersBeforeConvertedDateFilter() {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(FieldTypeConstants.TIME, FieldTypeConstants.STRING, "TEXT", "yyyy/MM/dd");
        DatasetRowPermissionsTreeItem item = new DatasetRowPermissionsTreeItem();
        item.setType("item");
        item.setField(field);
        item.setTerm("eq");
        item.setValue("2024-05-27");

        DatasetRowPermissionsTreeObj tree = new DatasetRowPermissionsTreeObj();
        tree.setLogic("AND");
        tree.setItems(List.of(item));

        DataSetRowPermissionsTreeDTO permission = new DataSetRowPermissionsTreeDTO();
        permission.setTree(tree);

        WhereTree2Str.transFilterTrees(meta, List.of(permission), List.of(field), false, null, null, null, null);

        assertContainsNormalizedTextOrigin(meta.getWhereTrees(), "t_a_0.`f_amount`");
        assertTrue(meta.getWhereTrees().contains("DE_STR_TO_DATE(CASE WHEN t_a_0.`f_amount` IS NULL THEN NULL"), meta.getWhereTrees());
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
        return datasetField(fieldType, fieldType, "DECIMAL", null);
    }

    private DatasetTableFieldDTO datasetField(Integer fieldType, Integer extractedFieldType, String type, String dateFormat) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(1L);
        field.setOriginName("amount");
        field.setEngineFieldName("f_amount");
        field.setFieldType(fieldType);
        field.setExtractedFieldType(extractedFieldType);
        field.setType(type);
        field.setDateFormat(dateFormat);
        return field;
    }

    private ChartViewFieldFilterDTO filter(String term, String value) {
        ChartViewFieldFilterDTO filter = new ChartViewFieldFilterDTO();
        filter.setTerm(term);
        filter.setValue(value);
        return filter;
    }

    private void assertContainsNormalizedTextOrigin(String sql, String origin) {
        assertTrue(sql.contains("CASE WHEN " + origin + " IS NULL THEN NULL"), sql);
        assertTrue(sql.contains("WHEN LOWER(TRIM(" + origin + ")) IN ('', 'null', '\\\\n') THEN NULL"), sql);
        assertTrue(sql.contains("ELSE TRIM(" + origin + ") END"), sql);
    }
}
