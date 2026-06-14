package io.crest.dataset.manage;

import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.TableField;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetDataManageFieldTypeTest {

    @Test
    void transFieldsUsesNativeTypeWhenLegacyExcelFieldHasNoSqlType() {
        TableField field = new TableField();
        field.setName("数量");
        field.setOriginName("数量");
        field.setDbFieldName("f_qty");
        field.setNativeType("LONG");

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(field), true);

        assertEquals(1, fields.size());
        DatasetTableFieldDTO result = fields.get(0);
        assertEquals("LONG", result.getType());
        assertEquals(2, result.getFieldType());
        assertEquals(2, result.getExtractedFieldType());
        assertEquals("q", result.getGroupType());
    }

    @Test
    void transFieldsDefaultsBlankExcelTypesToTextDimension() {
        TableField field = new TableField();
        field.setName("日期文本");
        field.setOriginName("日期文本");
        field.setDbFieldName("f_date_text");
        field.setType(" ");
        field.setNativeType(null);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(field), true);

        assertEquals(1, fields.size());
        DatasetTableFieldDTO result = fields.get(0);
        assertEquals("TEXT", result.getType());
        assertEquals(0, result.getFieldType());
        assertEquals(0, result.getExtractedFieldType());
        assertEquals("d", result.getGroupType());
    }

    @Test
    void transFieldsKeepsNullPreviewCellsIndependentFromFieldTypeResolution() {
        TableField field = new TableField();
        field.setName("可空日期");
        field.setOriginName("可空日期");
        field.setDbFieldName("f_nullable_day");
        field.setType(null);
        field.setNativeType("TEXT");

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(field), false);

        DatasetTableFieldDTO result = fields.get(0);
        assertEquals("TEXT", result.getType());
        assertEquals(0, result.getFieldType());
        assertEquals(0, result.getExtractedFieldType());
        assertEquals("d", result.getGroupType());
    }

    @Test
    void transFieldsNormalizesMixedExcelFieldListWithNullTypesAndExplicitOverrides() {
        TableField day = tableField("投产日期", "f_day", null, "DATE", null, null);
        TableField amount = tableField("金额", "f_amount", "DECIMAL(18,2)", null, null, null);
        TableField count = tableField("需求数", "f_count", "BIGINT(20)", null, null, null);
        TableField flag = tableField("是否超阈", "f_flag", "TINYINT(1)", null, null, null);
        TableField empty = tableField("空值列", "f_empty", null, null, null, null);
        TableField manualDimension = tableField("人工维度", "f_manual", "DOUBLE", null, 0, 3);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(
                List.of(day, amount, count, flag, empty, manualDimension),
                true
        );

        assertResolvedField(fields.get(0), "DATE", 1, 1, "d");
        assertResolvedField(fields.get(1), "DECIMAL(18,2)", 3, 3, "q");
        assertResolvedField(fields.get(2), "BIGINT(20)", 2, 2, "q");
        assertResolvedField(fields.get(3), "TINYINT(1)", 4, 4, "q");
        assertResolvedField(fields.get(4), "TEXT", 0, 0, "d");
        assertResolvedField(fields.get(5), "DOUBLE", 0, 3, "d");
    }

    private TableField tableField(String name, String dbFieldName, String type, String nativeType, Integer fieldType, Integer extractedFieldType) {
        TableField field = new TableField();
        field.setName(name);
        field.setOriginName(name);
        field.setDbFieldName(dbFieldName);
        field.setType(type);
        field.setNativeType(nativeType);
        field.setFieldType(fieldType);
        field.setExtractedFieldType(extractedFieldType);
        return field;
    }

    private void assertResolvedField(DatasetTableFieldDTO field, String type, int fieldType, int extractedFieldType, String groupType) {
        assertEquals(type, field.getType());
        assertEquals(fieldType, field.getFieldType());
        assertEquals(extractedFieldType, field.getExtractedFieldType());
        assertEquals(groupType, field.getGroupType());
    }
}
