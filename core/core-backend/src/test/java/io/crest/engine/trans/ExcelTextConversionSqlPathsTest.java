package io.crest.engine.trans;

import io.crest.api.chart.dto.ChartSortFieldDTO;
import io.crest.constant.FieldTypeConstants;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ExcelTextConversionSqlPathsTest {

    @ParameterizedTest
    @MethodSource("convertedExcelTextFields")
    void datasetPreviewFieldsNormalizeExcelTextPlaceholdersBeforeConversion(
            Integer fieldType,
            String dateFormat,
            String expectedPrefix
    ) {
        SQLMeta meta = meta();
        DatasetTableFieldDTO field = datasetField(fieldType, dateFormat);

        Field2SQLObj.field2sqlObj(meta, List.of(field), List.of(field), false, null, null, null, null);

        String fieldSql = meta.getXFields().get(0).getFieldName();
        assertContainsNormalizedTextOrigin(fieldSql, "t_a_0.`f_value`");
        assertTrue(fieldSql.contains(expectedPrefix), fieldSql);
    }

    @ParameterizedTest
    @MethodSource("convertedExcelTextFields")
    void chartDimensionsNormalizeExcelTextPlaceholdersBeforeConversion(
            Integer fieldType,
            String dateFormat,
            String expectedPrefix
    ) {
        SQLMeta meta = meta();
        ChartViewFieldDTO field = chartField(fieldType, dateFormat);
        field.setDateStyle("y_M_d");
        field.setDatePattern("date_sub");

        Dimension2SQLObj.dimension2sqlObj(meta, List.of(field), List.of(), false, null, null, null, null);

        String fieldSql = meta.getXFields().get(0).getFieldName();
        assertContainsNormalizedTextOrigin(fieldSql, "t_a_0.`f_value`");
        assertTrue(fieldSql.contains(expectedPrefix), fieldSql);
    }

    @ParameterizedTest
    @MethodSource("convertedExcelTextFields")
    void chartSortFieldsNormalizeExcelTextPlaceholdersBeforeConversion(
            Integer fieldType,
            String dateFormat,
            String expectedPrefix
    ) {
        SQLMeta meta = meta();
        ChartSortFieldDTO field = sortField(fieldType, dateFormat);

        Order2SQLObj.getOrders(meta, List.of(field), List.of(), false, null, null, null, null);

        String orderSql = meta.getXOrders().get(0).getOrderField();
        assertContainsNormalizedTextOrigin(orderSql, "t_a_0.`f_value`");
        assertTrue(orderSql.contains(expectedPrefix), orderSql);
    }

    private SQLMeta meta() {
        SQLMeta meta = new SQLMeta();
        meta.setTable(SQLObj.builder().tableAlias("t_a_0").build());
        return meta;
    }

    private DatasetTableFieldDTO datasetField(Integer fieldType, String dateFormat) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        setFieldBasics(field, fieldType, dateFormat);
        return field;
    }

    private ChartViewFieldDTO chartField(Integer fieldType, String dateFormat) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        setFieldBasics(field, fieldType, dateFormat);
        return field;
    }

    private ChartSortFieldDTO sortField(Integer fieldType, String dateFormat) {
        ChartSortFieldDTO field = new ChartSortFieldDTO();
        setFieldBasics(field, fieldType, dateFormat);
        field.setOrderDirection("asc");
        return field;
    }

    private void setFieldBasics(DatasetTableFieldDTO field, Integer fieldType, String dateFormat) {
        field.setId(1L);
        field.setOriginName("value");
        field.setName("value");
        field.setEngineFieldName("f_value");
        field.setFieldType(fieldType);
        field.setExtractedFieldType(FieldTypeConstants.STRING);
        field.setType("TEXT");
        field.setDateFormat(dateFormat);
    }

    private void assertContainsNormalizedTextOrigin(String sql, String origin) {
        assertTrue(sql.contains("CASE WHEN " + origin + " IS NULL THEN NULL"), sql);
        assertTrue(sql.contains("WHEN LOWER(TRIM(" + origin + ")) IN ('', 'null', '\\\\n') THEN NULL"), sql);
        assertTrue(sql.contains("ELSE TRIM(" + origin + ") END"), sql);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> convertedExcelTextFields() {
        return Stream.of(
                arguments(FieldTypeConstants.TIME, "yyyy/MM/dd", "DE_STR_TO_DATE("),
                arguments(FieldTypeConstants.INTEGER, null, "CAST("),
                arguments(FieldTypeConstants.FLOAT, null, "CAST(")
        );
    }
}
