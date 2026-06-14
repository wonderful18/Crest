package io.crest.dataset.manage;

import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.TableField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "项目编号|f_project_code|TEXT||0|0|d",
            "需求编号|f_req_code|TEXT||0|0|d",
            "投产月份|f_release_month|TEXT||0|0|d",
            "投产日期|f_release_day|TEXT||1|0|d",
            "需求数|f_req_count|LONG||2|2|q",
            "超期数量|f_overdue_count|BIGINT(20)||2|2|q",
            "平均耗时|f_avg_days|DOUBLE||3|3|q",
            "阈值|f_threshold|DECIMAL(18,2)||3|3|q",
            "是否重点|f_key_flag|TINYINT(1)||4|4|q",
            "空值列|f_empty|||0|0|d"
    })
    void transFieldsCoversWideExcelFieldTypeMatrix(
            String name,
            String dbFieldName,
            String type,
            String nativeType,
            Integer fieldType,
            Integer extractedFieldType,
            String groupType
    ) {
        TableField field = tableField(name, dbFieldName, blankToNull(type), blankToNull(nativeType), fieldType, extractedFieldType);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(field), true);

        String expectedType = blankToNull(type) == null && blankToNull(nativeType) == null ? "TEXT" : blankToNull(type);
        assertResolvedField(fields.get(0), expectedType, fieldType, extractedFieldType, groupType);
    }

    @Test
    void transFieldsPreservesManualTextDateConversionState() {
        TableField textDate = tableField("投产日期", "f_release_day", "TEXT", "TEXT", 1, 0);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(textDate), true);

        assertResolvedField(fields.get(0), "TEXT", 1, 0, "d");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "项目版本投产时间|f_version_release_day|TEXT|TEXT|1|0|d",
            "项目版本提测时间|f_version_test_day|TEXT|TEXT|1|0|d",
            "需求确认日期|f_confirm_day|TEXT|TEXT|1|0|d",
            "需求受理日期|f_accept_day|TEXT|TEXT|1|0|d",
            "需求分析完成日期|f_analysis_done_day|TEXT|TEXT|1|0|d",
            "开发预估工作量|f_dev_workload|TEXT|TEXT|3|0|q",
            "架构师评估工作量|f_arch_workload|TEXT|TEXT|3|0|q",
            "暂停时长|f_pause_days|TEXT|TEXT|2|0|q",
            "项目暂停时长|f_project_pause_days|TEXT|TEXT|2|0|q",
            "需求提出到项目终投产天数|f_req_to_final_days|TEXT|TEXT|2|0|q",
            "需求首次投产到需求确认天数|f_first_to_confirm_days|TEXT|TEXT|2|0|q",
            "空值列|f_empty|TEXT|TEXT|0|0|d"
    })
    void transFieldsPreservesManualConversionsFromTextBackedExcelColumns(
            String name,
            String dbFieldName,
            String type,
            String nativeType,
            Integer fieldType,
            Integer extractedFieldType,
            String groupType
    ) {
        TableField field = tableField(name, dbFieldName, type, nativeType, fieldType, extractedFieldType);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(field), true);

        assertResolvedField(fields.get(0), type, fieldType, extractedFieldType, groupType);
    }

    @Test
    void transFieldsHandlesRepresentativeRequirementProductionFieldListWithNullOnlyColumns() {
        List<TableField> tableFields = List.of(
                tableField("项目编号", "f_project_code", "TEXT", "TEXT", 0, 0),
                tableField("项目版本投产时间", "f_release_day", "TEXT", "TEXT", 1, 0),
                tableField("开发预估工作量", "f_dev_workload", "DOUBLE", "DOUBLE", 3, 3),
                tableField("架构师评估工作量", "f_arch_workload", "DOUBLE", "DOUBLE", 3, 3),
                tableField("暂停时长", "f_pause_days", "LONG", "LONG", 2, 2),
                tableField("需求提出到项目终投产天数", "f_req_to_final_days", "LONG", "LONG", 2, 2),
                tableField("交付物名称", "f_deliverable_name", "TEXT", "TEXT", 0, 0),
                tableField("项目恢复时间", "f_project_resume_day", "TEXT", "TEXT", 1, 0),
                tableField("说明", "f_note", null, null, null, null)
        );

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(tableFields, true);

        assertEquals(9, fields.size());
        assertResolvedField(fields.get(0), "TEXT", 0, 0, "d");
        assertResolvedField(fields.get(1), "TEXT", 1, 0, "d");
        assertResolvedField(fields.get(2), "DOUBLE", 3, 3, "q");
        assertResolvedField(fields.get(3), "DOUBLE", 3, 3, "q");
        assertResolvedField(fields.get(4), "LONG", 2, 2, "q");
        assertResolvedField(fields.get(5), "LONG", 2, 2, "q");
        assertResolvedField(fields.get(6), "TEXT", 0, 0, "d");
        assertResolvedField(fields.get(7), "TEXT", 1, 0, "d");
        assertResolvedField(fields.get(8), "TEXT", 0, 0, "d");
    }

    @Test
    void transFieldsHandlesWideRequirementConversionMatrixWithNullableExcelTypes() {
        Object[][] fieldCases = {
                {"项目编号", "f_project_code", "TEXT", "TEXT", 0, 0, "d"},
                {"项目类型", "f_project_type", "TEXT", "TEXT", 0, 0, "d"},
                {"项目级别", "f_project_level", "TEXT", "TEXT", 0, 0, "d"},
                {"项目经理", "f_project_manager", "TEXT", "TEXT", 0, 0, "d"},
                {"项目状态", "f_project_status", "TEXT", "TEXT", 0, 0, "d"},
                {"项目规模", "f_project_size", "TEXT", "TEXT", 0, 0, "d"},
                {"归属处室", "f_department", "TEXT", "TEXT", 0, 0, "d"},
                {"开发预估工作量", "f_dev_workload", "DOUBLE", "DOUBLE", 3, 3, "q"},
                {"架构师评估工作量", "f_arch_workload", "DOUBLE", "DOUBLE", 3, 3, "q"},
                {"项目版本", "f_project_version", "TEXT", "TEXT", 0, 0, "d"},
                {"发版类型", "f_release_type", "TEXT", "TEXT", 0, 0, "d"},
                {"项目版本投产时间", "f_version_release_day", "TEXT", "TEXT", 1, 0, "d"},
                {"项目版本提测时间", "f_version_test_day", "TEXT", "TEXT", 1, 0, "d"},
                {"项目首次投产日期", "f_project_first_release_day", "TEXT", "TEXT", 1, 0, "d"},
                {"项目最终投产日期", "f_project_final_release_day", "TEXT", "TEXT", 1, 0, "d"},
                {"交付物名称", "f_deliverable_name", "TEXT", "TEXT", 0, 0, "d"},
                {"验收测试报告提交时间", "f_report_submit_day", "TEXT", "TEXT", 1, 0, "d"},
                {"项目暂停时间", "f_project_pause_day", "TEXT", "TEXT", 1, 0, "d"},
                {"项目恢复时间", "f_project_resume_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求标题", "f_requirement_title", "TEXT", "TEXT", 0, 0, "d"},
                {"需求编号", "f_requirement_code", "TEXT", "TEXT", 0, 0, "d"},
                {"需求状态", "f_requirement_status", "TEXT", "TEXT", 0, 0, "d"},
                {"范围变更时间", "f_scope_change_day", "TEXT", "TEXT", 1, 0, "d"},
                {"立项通过时间", "f_approval_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求立项时间", "f_requirement_start_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求提出部门审批日期", "f_department_approval_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求首次投产时间", "f_requirement_first_release_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求确认日期", "f_requirement_confirm_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求受理日期", "f_requirement_accept_day", "TEXT", "TEXT", 1, 0, "d"},
                {"需求分析完成日期", "f_analysis_done_day", "TEXT", "TEXT", 1, 0, "d"},
                {"暂停时长", "f_pause_days", "LONG", "LONG", 2, 2, "q"},
                {"项目暂停时长", "f_project_pause_days", "LONG", "LONG", 2, 2, "q"},
                {"需求提出到项目首投产天数", "f_req_to_project_first_days", "LONG", "LONG", 2, 2, "q"},
                {"需求提出到项目终投产天数", "f_req_to_project_final_days", "LONG", "LONG", 2, 2, "q"},
                {"需求确认到项目首投产天数", "f_confirm_to_project_first_days", "LONG", "LONG", 2, 2, "q"},
                {"需求确认到项目终投产天数", "f_confirm_to_project_final_days", "LONG", "LONG", 2, 2, "q"},
                {"需求受理到首次投产天数", "f_accept_to_first_days", "LONG", "LONG", 2, 2, "q"},
                {"需求受理到最终投产天数", "f_accept_to_final_days", "LONG", "LONG", 2, 2, "q"},
                {"需求分析完成到首次投产天数", "f_analysis_to_first_days", "LONG", "LONG", 2, 2, "q"},
                {"需求分析完成到最终投产天数", "f_analysis_to_final_days", "LONG", "LONG", 2, 2, "q"},
                {"需求首次投产到需求确认天数", "f_first_to_confirm_days", "LONG", "LONG", 2, 2, "q"},
                {"空值列", "f_empty", null, null, null, null, "d"}
        };
        List<TableField> tableFields = java.util.Arrays.stream(fieldCases)
                .map(item -> tableField((String) item[0], (String) item[1], (String) item[2], (String) item[3], (Integer) item[4], (Integer) item[5]))
                .toList();

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(tableFields, true);

        assertEquals(fieldCases.length, fields.size());
        for (int i = 0; i < fieldCases.length; i++) {
            DatasetTableFieldDTO field = fields.get(i);
            assertEquals(fieldCases[i][0], field.getName());
            assertEquals(fieldCases[i][1], field.getDbFieldName());
            String expectedType = fieldCases[i][2] == null ? "TEXT" : (String) fieldCases[i][2];
            int expectedFieldType = fieldCases[i][4] == null ? 0 : (Integer) fieldCases[i][4];
            int expectedExtractedFieldType = fieldCases[i][5] == null ? 0 : (Integer) fieldCases[i][5];
            assertResolvedField(field, expectedType, expectedFieldType, expectedExtractedFieldType, (String) fieldCases[i][6]);
        }
    }

    @Test
    void transFieldsKeepsNullTypeColumnsAsTextUntilUserChoosesConversion() {
        TableField nullOnly = tableField("空值列", "f_empty", null, null, null, null);

        List<DatasetTableFieldDTO> fields = new DatasetDataManage().transFields(List.of(nullOnly), true);

        assertResolvedField(fields.get(0), "TEXT", 0, 0, "d");
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void assertResolvedField(DatasetTableFieldDTO field, String type, int fieldType, int extractedFieldType, String groupType) {
        assertEquals(type, field.getType());
        assertEquals(fieldType, field.getFieldType());
        assertEquals(extractedFieldType, field.getExtractedFieldType());
        assertEquals(groupType, field.getGroupType());
    }
}
