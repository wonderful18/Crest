package io.crest.datasource.provider;

import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.api.ds.vo.ExcelSheetData;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.extensions.datasource.dto.TableFieldWithValue;
import io.crest.datasource.server.DatasourceServer;
import io.crest.exception.CrestException;
import io.crest.utils.JsonUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelFieldNameTest {

    @Test
    void excelDbFieldNameUsesShortStableIdentifierForComplexHeader() {
        String header = "需求业务审批会签耗时（天）【需求提出-业务部门会签完成后出需求】";

        String dbFieldName = ExcelUtils.excelDbFieldName(header);

        assertTrue(dbFieldName.startsWith("f_"));
        assertTrue(dbFieldName.length() <= 64);
        assertEquals(dbFieldName, ExcelUtils.excelDbFieldName(header));
        assertFalse(dbFieldName.contains("需求业务审批"));
    }

    @Test
    void mysqlCreateTableUsesDbFieldNameWhenPresent() {
        TableField field = checkedTextField("需求业务审批会签耗时（天）【需求提出-业务部门会签完成后出需求】", "f_excel_field_001");

        String sql = new MysqlEngineProvider().createTableSql("excel_Sheet1_4e0cd50314", java.util.List.of(field), engine("mysql"));

        assertTrue(sql.contains("`f_excel_field_001` longtext"));
        assertFalse(sql.contains("`需求业务审批会签耗时"));
    }

    @Test
    void h2CreateTableUsesDbFieldNameWhenPresent() {
        TableField field = checkedTextField("SELECT distinct a.code as 项目编号,bb.name as 项目类型,t.name as 项目级别,ad.displayname", "f_excel_field_002");

        String sql = new H2EngineProvider().createTableSql("excel_Sheet1_4e0cd50314", java.util.List.of(field), engine("h2"));

        assertTrue(sql.contains("\"f_excel_field_002\" longtext"));
        assertFalse(sql.contains("\"SELECT distinct"));
    }

    @Test
    void excelImportSkipsSqlDescriptionSheetAndKeepsDataSheet() throws Exception {
        List<ExcelSheetData> sheets = parseWorkbook(workbookWithSqlDescriptionSheet());

        assertEquals(2, sheets.size());
        ExcelSheetData descriptionSheet = sheets.get(0);
        assertFalse(descriptionSheet.isSheet());
        assertEquals("SKIPPED", descriptionSheet.getInspectionStatus());
        assertEquals("检测到该工作表首行疑似 SQL 或说明文本，默认不导入。", descriptionSheet.getInspectionMessage());
        assertTrue(descriptionSheet.getFields().isEmpty());
        assertTrue(descriptionSheet.getJsonArray().isEmpty());

        ExcelSheetData dataSheet = sheets.get(1);
        assertTrue(dataSheet.isSheet());
        assertEquals("NORMAL", dataSheet.getInspectionStatus());
        assertEquals(2, dataSheet.getFields().size());
        assertEquals("月份", dataSheet.getFields().get(0).getName());
        assertNotNull(dataSheet.getFields().get(0).getDbFieldName());
        assertEquals(1, dataSheet.getJsonArray().size());
    }

    @Test
    void excelImportDoesNotSkipTabularHeaderContainingSqlWords() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("select_flag");
            header.createCell(1).setCellValue("from_channel");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Y");
            row.createCell(1).setCellValue("portal");

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            assertEquals(1, sheets.size());
            assertTrue(sheets.get(0).isSheet());
            assertEquals("NORMAL", sheets.get(0).getInspectionStatus());
            assertEquals(2, sheets.get(0).getFields().size());
        }
    }

    @Test
    void excelImportMakesDuplicateHeadersUnique() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("重复表头");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("指标");
            header.createCell(1).setCellValue("指标");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("A");
            row.createCell(1).setCellValue("B");

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            assertEquals(1, sheets.size());
            ExcelSheetData dataSheet = sheets.get(0);
            assertTrue(dataSheet.isSheet());
            assertEquals("指标", dataSheet.getFields().get(0).getName());
            assertEquals("指标_2", dataSheet.getFields().get(1).getName());
            assertFalse(dataSheet.getFields().get(0).getDbFieldName().equals(dataSheet.getFields().get(1).getDbFieldName()));
            assertEquals("A", dataSheet.getJsonArray().get(0).get("指标"));
            assertEquals("B", dataSheet.getJsonArray().get(0).get("指标_2"));
        }
    }

    @Test
    void excelImportStoresCanonicalFieldType() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("字段类型");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("月份");
            header.createCell(1).setCellValue("数量");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026年1月");
            row.createCell(1).setCellValue(12);

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            ExcelSheetData dataSheet = sheets.get(0);
            assertEquals("TEXT", dataSheet.getFields().get(0).getNativeType());
            assertEquals("TEXT", dataSheet.getFields().get(0).getType());
            assertEquals("LONG", dataSheet.getFields().get(1).getNativeType());
            assertEquals("LONG", dataSheet.getFields().get(1).getType());
        }
    }

    @Test
    void excelImportDefaultsColumnsWithLeadingBlankCellsToSafeTextType() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("空值");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("日期");
            header.createCell(1).setCellValue("数量");
            Row row1 = sheet.createRow(1);
            row1.createCell(1).setCellValue(5);
            Row row = sheet.createRow(2);
            row.createCell(0).setCellValue("2026/06/14");
            row.createCell(1).setCellValue(10);

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            ExcelSheetData dataSheet = sheets.get(0);
            assertEquals("TEXT", dataSheet.getFields().get(0).getType());
            assertEquals("LONG", dataSheet.getFields().get(1).getType());
            assertEquals(null, dataSheet.getJsonArray().get(0).get("日期"));
            assertEquals("5", dataSheet.getJsonArray().get(0).get("数量"));
        }
    }

    @Test
    void excelImportPromotesMixedNumericAndTextColumnsToText() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("混合类型");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("指标值");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(12);
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("暂无");

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            ExcelSheetData dataSheet = sheets.get(0);
            assertEquals("TEXT", dataSheet.getFields().get(0).getType());
            assertEquals(0, dataSheet.getFields().get(0).getFieldType());
            assertEquals(0, dataSheet.getFields().get(0).getExtractedFieldType());
        }
    }

    @Test
    void excelImportHandlesWideSheetWithNullsAndMixedFieldTypes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("宽表");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "月份",
                    "处室",
                    "版本号",
                    "需求数",
                    "平均耗时",
                    "超期数量",
                    "投产日期",
                    "日期文本",
                    "长编码",
                    "负数指标",
                    "百分比文本",
                    "空值指标",
                    "金额",
                    "是否重点",
                    "说明",
                    "科学计数文本",
                    "带空格字段",
                    "重复指标",
                    "重复指标"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("2026年1月");
            row1.createCell(1).setCellValue("研发一室");
            row1.createCell(2).setCellValue("V1.0");
            row1.createCell(3).setCellValue(12);
            row1.createCell(4).setCellValue(18.5);
            row1.createCell(6).setCellValue("2026/06/14");
            row1.createCell(7).setCellValue("20260614");
            row1.createCell(8).setCellValue("123456789012345678901");
            row1.createCell(9).setCellValue(-3);
            row1.createCell(10).setCellValue("12.5%");
            row1.createCell(12).setCellValue(123456.78);
            row1.createCell(13).setCellValue(true);
            row1.createCell(14).setCellValue("包含逗号, 括号() 与较长说明");
            row1.createCell(15).setCellValue("1.23E4");
            row1.createCell(16).setCellValue("合同");
            row1.createCell(17).setCellValue(7);
            row1.createCell(18).setCellValue("B");

            Row row2 = sheet.createRow(2);
            row2.createCell(1).setCellValue("研发二室");
            row2.createCell(2).setCellValue("V1.1");
            row2.createCell(3).setCellValue("待定");
            row2.createCell(4).setCellValue(20.25);
            row2.createCell(5).setCellValue(2);
            row2.createCell(6).setCellValue("无");
            row2.createCell(7).setCellValue("20260701");
            row2.createCell(8).setCellValue("ABCD");
            row2.createCell(9).setCellValue(-4);
            row2.createCell(10).setCellValue("0%");
            row2.createCell(12).setCellValue(0);
            row2.createCell(13).setCellValue(false);
            row2.createCell(15).setCellValue("9.9E-2");
            row2.createCell(16).setCellValue("上线");
            row2.createCell(17).setCellValue(8);
            row2.createCell(18).setCellValue("C");

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            ExcelSheetData dataSheet = sheets.get(0);
            assertEquals(headers.length, dataSheet.getFields().size());
            assertEquals("重复指标", dataSheet.getFields().get(17).getName());
            assertEquals("重复指标_2", dataSheet.getFields().get(18).getName());
            assertField(dataSheet, "月份", "TEXT", 0);
            assertField(dataSheet, "处室", "TEXT", 0);
            assertField(dataSheet, "版本号", "TEXT", 0);
            assertField(dataSheet, "需求数", "TEXT", 0);
            assertField(dataSheet, "平均耗时", "DOUBLE", 3);
            assertField(dataSheet, "超期数量", "LONG", 2);
            assertField(dataSheet, "投产日期", "TEXT", 0);
            assertField(dataSheet, "日期文本", "TEXT", 0);
            assertField(dataSheet, "长编码", "TEXT", 0);
            assertField(dataSheet, "负数指标", "LONG", 2);
            assertField(dataSheet, "百分比文本", "TEXT", 0);
            assertField(dataSheet, "空值指标", "TEXT", 0);
            assertField(dataSheet, "金额", "DOUBLE", 3);
            assertField(dataSheet, "是否重点", "TEXT", 0);
            assertField(dataSheet, "说明", "TEXT", 0);
            assertField(dataSheet, "科学计数文本", "TEXT", 0);
            assertField(dataSheet, "重复指标", "LONG", 2);
            assertField(dataSheet, "重复指标_2", "TEXT", 0);

            assertEquals(null, dataSheet.getJsonArray().get(0).get("超期数量"));
            assertEquals(null, dataSheet.getJsonArray().get(0).get("空值指标"));
            assertEquals(null, dataSheet.getJsonArray().get(1).get("月份"));
            assertEquals("研发二室", dataSheet.getJsonArray().get(1).get("处室"));
            assertEquals("待定", dataSheet.getJsonArray().get(1).get("需求数"));
            assertEquals("2", dataSheet.getJsonArray().get(1).get("超期数量"));
        }
    }

    @Test
    void excelImportHandlesLargeSparseSheetWithoutColumnShift() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("复杂宽表");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "月份",
                    "处室",
                    "需求编号",
                    "版本编号",
                    "项目编号",
                    "项目名称",
                    "项目级别",
                    "项目类型",
                    "是否重点",
                    "需求状态",
                    "计划投产日期",
                    "实际投产日期",
                    "需求数",
                    "投产版本数",
                    "平均耗时",
                    "最长耗时",
                    "超期数量",
                    "阈值",
                    "风险说明",
                    "备注",
                    "金额",
                    "占比",
                    "负数",
                    "长编码",
                    "科学计数文本",
                    "空列指标",
                    "日期文本",
                    "数字文本",
                    "布尔文本",
                    "公式结果文本",
                    "分类1",
                    "分类2",
                    "分类3",
                    "分类4",
                    "分类5",
                    "指标A",
                    "指标B",
                    "指标C",
                    "指标D",
                    "指标E",
                    "重复指标",
                    "重复指标",
                    "SQL片段文本",
                    "含空格 字段",
                    "特殊符号(天)"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("2026年1月");
            row1.createCell(1).setCellValue("研发一室");
            row1.createCell(2).setCellValue("REQ-001");
            row1.createCell(3).setCellValue("V1.0");
            row1.createCell(12).setCellValue(12);
            row1.createCell(13).setCellValue(3);
            row1.createCell(14).setCellValue(18.5);
            row1.createCell(15).setCellValue(32);
            row1.createCell(17).setCellValue(30.5);
            row1.createCell(20).setCellValue(123456.78);
            row1.createCell(21).setCellValue("12.5%");
            row1.createCell(22).setCellValue(-5);
            row1.createCell(23).setCellValue("123456789012345678901");
            row1.createCell(24).setCellValue("1.23E4");
            row1.createCell(26).setCellValue("2026/06/14");
            row1.createCell(27).setCellValue("00123");
            row1.createCell(28).setCellValue("Y");
            row1.createCell(35).setCellValue(1);
            row1.createCell(40).setCellValue(7);
            row1.createCell(41).setCellValue("重复文本");
            row1.createCell(42).setCellValue("SELECT a FROM t");
            row1.createCell(44).setCellValue("右侧字段值");

            Row row2 = sheet.createRow(2);
            row2.createCell(1).setCellValue("研发二室");
            row2.createCell(2).setCellValue("REQ-002");
            row2.createCell(3).setCellValue("V1.1");
            row2.createCell(12).setCellValue("待定");
            row2.createCell(13).setCellValue(4);
            row2.createCell(14).setCellValue(20.25);
            row2.createCell(16).setCellValue(2);
            row2.createCell(17).setCellValue(30);
            row2.createCell(18).setCellValue("超过阈值");
            row2.createCell(20).setCellValue(0);
            row2.createCell(22).setCellValue(-1);
            row2.createCell(23).setCellValue("ABCD");
            row2.createCell(26).setCellValue("无");
            row2.createCell(27).setCellValue(456);
            row2.createCell(28).setCellValue(false);
            row2.createCell(35).setCellValue(2.5);
            row2.createCell(40).setCellValue(8);
            row2.createCell(41).setCellValue("重复文本2");
            row2.createCell(44).setCellValue("右侧字段值2");

            Row row3 = sheet.createRow(3);
            row3.createCell(2).setCellValue("REQ-003");
            row3.createCell(16).setCellValue(0);
            row3.createCell(35).setCellValue(3);
            row3.createCell(44).setCellValue("只填最右列");

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            ExcelSheetData dataSheet = sheets.get(0);
            assertEquals(headers.length, dataSheet.getFields().size());
            assertEquals(3, dataSheet.getJsonArray().size());
            assertEquals(null, dataSheet.getJsonArray().get(1).get("月份"));
            assertEquals("研发二室", dataSheet.getJsonArray().get(1).get("处室"));
            assertEquals("REQ-002", dataSheet.getJsonArray().get(1).get("需求编号"));
            assertEquals("只填最右列", dataSheet.getJsonArray().get(2).get("特殊符号(天)"));
            assertEquals(null, dataSheet.getJsonArray().get(2).get("处室"));
            assertEquals("重复指标_2", dataSheet.getFields().get(41).getName());
            assertField(dataSheet, "需求数", "TEXT", 0);
            assertField(dataSheet, "投产版本数", "LONG", 2);
            assertField(dataSheet, "平均耗时", "DOUBLE", 3);
            assertField(dataSheet, "超期数量", "LONG", 2);
            assertField(dataSheet, "空列指标", "TEXT", 0);
            assertField(dataSheet, "金额", "DOUBLE", 3);
            assertField(dataSheet, "长编码", "TEXT", 0);

            Set<String> dbFieldNames = new HashSet<>();
            for (TableField field : dataSheet.getFields()) {
                assertNotNull(field.getType());
                assertNotNull(field.getNativeType());
                assertNotNull(field.getFieldType());
                assertNotNull(field.getExtractedFieldType());
                assertTrue(field.getDbFieldName().length() <= 64);
                assertTrue(dbFieldNames.add(field.getDbFieldName()));
            }
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "项目编号|00123",
            "需求编号|202605150001",
            "需求编码|000089",
            "版本号|202606001",
            "工单ID|42",
            "code|0007",
            "dept_id|0088",
            "日期文本|20260614",
            "投产月份|202606",
            "投产年月|202601"
    })
    void excelImportKeepsIdentifierAndDateLikeNumericTextAsText(String headerName, String value) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("类型识别");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(headerName);
            header.createCell(1).setCellValue("需求数");
            header.createCell(2).setCellValue("平均耗时");
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue(value);
            first.createCell(1).setCellValue(" 12 ");
            first.createCell(2).setCellValue(" 18.50 ");
            Row second = sheet.createRow(2);
            second.createCell(1).setCellValue(0);
            second.createCell(2).setCellValue(-3.25);

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertField(dataSheet, headerName, "TEXT", 0);
            assertField(dataSheet, "需求数", "LONG", 2);
            assertField(dataSheet, "平均耗时", "DOUBLE", 3);
            assertEquals(value, dataSheet.getJsonArray().get(0).get(headerName));
            assertEquals("12", dataSheet.getJsonArray().get(0).get("需求数"));
            assertEquals("18.50", dataSheet.getJsonArray().get(0).get("平均耗时"));
        }
    }

    @Test
    void excelImportInfersWideOperationalSheetWithNullsIdentifiersAndMetrics() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("运营宽表");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "项目编号",
                    "项目名称",
                    "需求编号",
                    "处室",
                    "投产月份",
                    "投产日期",
                    "需求数",
                    "投产版本数",
                    "合并需求阶段耗时",
                    "业务审批耗时(天)",
                    "阈值",
                    "超期数量",
                    "风险等级",
                    "备注",
                    "长编码",
                    "科学计数文本",
                    "百分比文本",
                    "空值列",
                    "重复指标",
                    "重复指标"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("000001");
            first.createCell(1).setCellValue("核心升级");
            first.createCell(2).setCellValue("202605150001");
            first.createCell(3).setCellValue("研发一室");
            first.createCell(4).setCellValue("202606");
            first.createCell(5).setCellValue("2026/06/14");
            first.createCell(6).setCellValue(12);
            first.createCell(7).setCellValue(3);
            first.createCell(8).setCellValue(" 18 ");
            first.createCell(9).setCellValue(21.5);
            first.createCell(10).setCellValue(20);
            first.createCell(12).setCellValue("高");
            first.createCell(14).setCellValue("123456789012345678901");
            first.createCell(15).setCellValue("1.23E4");
            first.createCell(16).setCellValue("12.5%");
            first.createCell(18).setCellValue(7);
            first.createCell(19).setCellValue("A");

            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("000002");
            second.createCell(2).setCellValue("202605150002");
            second.createCell(3).setCellValue("研发二室");
            second.createCell(4).setCellValue("202607");
            second.createCell(6).setCellValue(0);
            second.createCell(7).setCellValue(4);
            second.createCell(8).setCellValue(-1);
            second.createCell(9).setCellValue("待确认");
            second.createCell(10).setCellValue(20.5);
            second.createCell(11).setCellValue(2);
            second.createCell(14).setCellValue("ABCD");
            second.createCell(18).setCellValue(8);
            second.createCell(19).setCellValue("B");

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertEquals(headers.length, dataSheet.getFields().size());
            assertField(dataSheet, "项目编号", "TEXT", 0);
            assertField(dataSheet, "需求编号", "TEXT", 0);
            assertField(dataSheet, "投产月份", "TEXT", 0);
            assertField(dataSheet, "投产日期", "TEXT", 0);
            assertField(dataSheet, "需求数", "LONG", 2);
            assertField(dataSheet, "投产版本数", "LONG", 2);
            assertField(dataSheet, "合并需求阶段耗时", "LONG", 2);
            assertField(dataSheet, "业务审批耗时(天)", "TEXT", 0);
            assertField(dataSheet, "阈值", "DOUBLE", 3);
            assertField(dataSheet, "超期数量", "LONG", 2);
            assertField(dataSheet, "长编码", "TEXT", 0);
            assertField(dataSheet, "科学计数文本", "TEXT", 0);
            assertField(dataSheet, "百分比文本", "TEXT", 0);
            assertField(dataSheet, "空值列", "TEXT", 0);
            assertField(dataSheet, "重复指标", "LONG", 2);
            assertField(dataSheet, "重复指标_2", "TEXT", 0);
            assertEquals(null, dataSheet.getJsonArray().get(0).get("超期数量"));
            assertEquals(null, dataSheet.getJsonArray().get(1).get("项目名称"));
            assertEquals("000002", dataSheet.getJsonArray().get(1).get("项目编号"));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "开发预估工作量|DOUBLE|3",
            "架构师评估工作量|DOUBLE|3",
            "金额|DOUBLE|3",
            "平均耗时|DOUBLE|3",
            "阈值|DOUBLE|3",
            "暂停时长|LONG|2"
    })
    void excelImportKeepsDecimalSemanticMetricFieldsAsDoubleWhenPreviewValuesAreWholeNumbers(
            String headerName,
            String expectedType,
            int expectedFieldType
    ) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("小数语义字段");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(headerName);
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue(174);
            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue(654);

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertField(dataSheet, headerName, expectedType, expectedFieldType);
        }
    }

    @Test
    void excelImportTreatsLiteralNullPlaceholdersAsMissingValuesWhenInferringTypes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("空值占位符");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "项目编号",
                    "开发预估工作量",
                    "暂停时长",
                    "项目版本投产时间",
                    "需求提出到项目首投产天数",
                    "说明"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("A20210186");
            first.createCell(1).setCellValue("199.00");
            first.createCell(2).setCellValue("null");
            first.createCell(3).setCellValue("2024/05/27");
            first.createCell(4).setCellValue("NULL");
            first.createCell(5).setCellValue("正常");

            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("A20210187");
            second.createCell(1).setCellValue("null");
            second.createCell(2).setCellValue("3");
            second.createCell(3).setCellValue("null");
            second.createCell(4).setCellValue("15");
            second.createCell(5).setCellValue("\\N");

            Row third = sheet.createRow(3);
            third.createCell(0).setCellValue("A20210188");
            third.createCell(1).setCellValue("200.50");
            third.createCell(2).setCellValue("0");
            third.createCell(3).setCellValue("2024/06/01");
            third.createCell(4).setCellValue("-2");
            third.createCell(5).setCellValue("包含 null 字样的正常说明");

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertField(dataSheet, "项目编号", "TEXT", 0);
            assertField(dataSheet, "开发预估工作量", "DOUBLE", 3);
            assertField(dataSheet, "暂停时长", "LONG", 2);
            assertField(dataSheet, "项目版本投产时间", "TEXT", 0);
            assertField(dataSheet, "需求提出到项目首投产天数", "LONG", 2);
            assertField(dataSheet, "说明", "TEXT", 0);
            assertNull(dataSheet.getJsonArray().get(0).get("暂停时长"));
            assertNull(dataSheet.getJsonArray().get(0).get("需求提出到项目首投产天数"));
            assertNull(dataSheet.getJsonArray().get(1).get("开发预估工作量"));
            assertNull(dataSheet.getJsonArray().get(1).get("项目版本投产时间"));
            assertNull(dataSheet.getJsonArray().get(1).get("说明"));
            assertEquals("200.50", dataSheet.getJsonArray().get(2).get("开发预估工作量"));
        }
    }

    @Test
    void excelImportInfersRepresentativeRequirementProductionExportWithManyNullableFields() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("需求投产明细");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "项目编号",
                    "项目类型",
                    "项目级别",
                    "项目经理",
                    "项目状态",
                    "项目规模",
                    "归属处室",
                    "开发预估工作量",
                    "架构师评估工作量",
                    "项目版本",
                    "发版类型",
                    "项目版本投产时间",
                    "项目版本提测时间",
                    "项目首次投产日期",
                    "项目最终投产日期",
                    "交付物名称",
                    "验收测试报告提交时间",
                    "项目暂停时间",
                    "项目恢复时间",
                    "需求标题",
                    "需求编号",
                    "需求状态",
                    "范围变更时间",
                    "立项通过时间",
                    "需求立项时间",
                    "需求提出部门审批日期",
                    "需求首次投产时间",
                    "需求确认日期",
                    "需求受理日期",
                    "需求分析完成日期",
                    "暂停时长",
                    "项目暂停时长",
                    "需求提出到项目首投产天数",
                    "需求提出到项目终投产天数",
                    "需求确认到项目首投产天数",
                    "需求确认到项目终投产天数",
                    "需求受理到首次投产天数",
                    "需求受理到最终投产天数",
                    "需求分析完成到首次投产天数",
                    "需求分析完成到最终投产天数",
                    "需求首次投产到需求确认天数"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("A20210186");
            first.createCell(1).setCellValue("非商务类项目模板");
            first.createCell(2).setCellValue("一般项目");
            first.createCell(3).setCellValue("项目经理A");
            first.createCell(4).setCellValue("已撤销");
            first.createCell(5).setCellValue("任务级");
            first.createCell(6).setCellValue("开发一室");
            first.createCell(7).setCellValue("199.00");
            first.createCell(8).setCellValue("174.00");
            first.createCell(9).setCellValue("2024年紧急版");
            first.createCell(10).setCellValue("紧急");
            first.createCell(11).setCellValue("2024/05/27");
            first.createCell(12).setCellValue("2024/05/01");
            first.createCell(13).setCellValue("2024/05/27");
            first.createCell(14).setCellValue("2024/05/27");
            first.createCell(15).setCellValue("null");
            first.createCell(16).setCellValue("null");
            first.createCell(17).setCellValue("null");
            first.createCell(18).setCellValue("null");
            first.createCell(19).setCellValue("需求标题A");
            first.createCell(20).setCellValue("REQ-202405270001");
            first.createCell(21).setCellValue("已投产");
            first.createCell(22).setCellValue("null");
            first.createCell(23).setCellValue("2024/01/15");
            first.createCell(24).setCellValue("2024/01/10");
            first.createCell(25).setCellValue("2024/01/08");
            first.createCell(26).setCellValue("2024/05/27");
            first.createCell(27).setCellValue("2024/01/09");
            first.createCell(28).setCellValue("2024/01/11");
            first.createCell(29).setCellValue("2024/01/20");
            first.createCell(30).setCellValue("null");
            first.createCell(31).setCellValue("null");
            first.createCell(32).setCellValue("140");
            first.createCell(33).setCellValue("140");
            first.createCell(34).setCellValue("139");
            first.createCell(35).setCellValue("139");
            first.createCell(36).setCellValue("137");
            first.createCell(37).setCellValue("137");
            first.createCell(38).setCellValue("128");
            first.createCell(39).setCellValue("128");
            first.createCell(40).setCellValue("-139");

            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("A20220005");
            second.createCell(1).setCellValue("非商务类项目模板");
            second.createCell(2).setCellValue("一般项目");
            second.createCell(3).setCellValue("项目经理B");
            second.createCell(4).setCellValue("进行中");
            second.createCell(5).setCellValue("小型项目");
            second.createCell(6).setCellValue("数据应用开发室");
            second.createCell(7).setCellValue("825.33");
            second.createCell(8).setCellValue("654.00");
            second.createCell(9).setCellValue("2022年5月版");
            second.createCell(10).setCellValue("月版");
            second.createCell(11).setCellValue("2022/05/21");
            second.createCell(12).setCellValue("2022/03/25");
            second.createCell(13).setCellValue("2022/05/21");
            second.createCell(14).setCellValue("2026/12/30");
            second.createCell(15).setCellValue("验收测试报告.doc");
            second.createCell(16).setCellValue("2024/06/05");
            second.createCell(17).setCellValue("NULL");
            second.createCell(18).setCellValue("\\N");
            second.createCell(19).setCellValue("需求标题B");
            second.createCell(20).setCellValue("REQ-202203250001");
            second.createCell(21).setCellValue("进行中");
            second.createCell(22).setCellValue("2022/04/01");
            second.createCell(23).setCellValue("2022/02/15");
            second.createCell(24).setCellValue("2022/02/10");
            second.createCell(25).setCellValue("2022/02/08");
            second.createCell(26).setCellValue("2022/05/21");
            second.createCell(27).setCellValue("2022/02/09");
            second.createCell(28).setCellValue("2022/02/11");
            second.createCell(29).setCellValue("2022/02/20");
            second.createCell(30).setCellValue("0");
            second.createCell(31).setCellValue("5");
            second.createCell(32).setCellValue("100");
            second.createCell(33).setCellValue("1774");
            second.createCell(34).setCellValue("101");
            second.createCell(35).setCellValue("1775");
            second.createCell(36).setCellValue("99");
            second.createCell(37).setCellValue("1773");
            second.createCell(38).setCellValue("90");
            second.createCell(39).setCellValue("1764");
            second.createCell(40).setCellValue("-101");

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertEquals(headers.length, dataSheet.getFields().size());
            assertEquals(2, dataSheet.getJsonArray().size());
            assertField(dataSheet, "项目编号", "TEXT", 0);
            assertField(dataSheet, "需求编号", "TEXT", 0);
            assertField(dataSheet, "项目版本投产时间", "TEXT", 0);
            assertField(dataSheet, "需求首次投产时间", "TEXT", 0);
            assertField(dataSheet, "开发预估工作量", "DOUBLE", 3);
            assertField(dataSheet, "架构师评估工作量", "DOUBLE", 3);
            assertField(dataSheet, "暂停时长", "LONG", 2);
            assertField(dataSheet, "项目暂停时长", "LONG", 2);
            assertField(dataSheet, "需求提出到项目首投产天数", "LONG", 2);
            assertField(dataSheet, "需求提出到项目终投产天数", "LONG", 2);
            assertField(dataSheet, "需求首次投产到需求确认天数", "LONG", 2);
            assertNull(dataSheet.getJsonArray().get(0).get("交付物名称"));
            assertNull(dataSheet.getJsonArray().get(0).get("暂停时长"));
            assertNull(dataSheet.getJsonArray().get(1).get("项目恢复时间"));
            assertEquals("1774", dataSheet.getJsonArray().get(1).get("需求提出到项目终投产天数"));
        }
    }

    @Test
    void excelImportCoversWideRequirementFieldMatrixWithBlankAndNullCells() throws Exception {
        Object[][] fieldCases = {
                {"项目编号", " null ", "A20210186", "A20220005", "TEXT", 0},
                {"项目类型", "", "非商务类项目模板", "一般项目", "TEXT", 0},
                {"项目级别", "   ", "一般项目", "重要项目", "TEXT", 0},
                {"项目经理", "\\N", "项目经理A", "项目经理B", "TEXT", 0},
                {"项目状态", "NULL", "已投产", "进行中", "TEXT", 0},
                {"项目规模", "Null", "任务级", "小型项目", "TEXT", 0},
                {"归属处室", "null", "开发一室", "数据应用开发室", "TEXT", 0},
                {"开发预估工作量", "null", "199.00", "825.33", "DOUBLE", 3},
                {"架构师评估工作量", "NULL", 174.00, 654.00, "DOUBLE", 3},
                {"项目版本", "\\N", "2024年紧急版", "2022年5月版", "TEXT", 0},
                {"发版类型", "null", "紧急", "月版", "TEXT", 0},
                {"项目版本投产时间", "NULL", "2024/05/27", "2022/05/21", "TEXT", 0},
                {"项目版本提测时间", " ", "2024/05/01", "2022/03/25", "TEXT", 0},
                {"项目首次投产日期", "\\N", "2024/05/27", "2022/05/21", "TEXT", 0},
                {"项目最终投产日期", "NULL", "2024/05/27", "2026/12/30", "TEXT", 0},
                {"交付物名称", "null", "验收测试报告.doc", "上线清单.xlsx", "TEXT", 0},
                {"验收测试报告提交时间", "", "2024/06/05", "2024/06/06", "TEXT", 0},
                {"项目暂停时间", "\\N", "2024/04/01", "2024/04/02", "TEXT", 0},
                {"项目恢复时间", "NULL", "2024/04/05", "2024/04/06", "TEXT", 0},
                {"需求标题", "   ", "需求标题A", "需求标题B", "TEXT", 0},
                {"需求编号", "null", "REQ-202405270001", "202203250001", "TEXT", 0},
                {"需求状态", "NULL", "已投产", "进行中", "TEXT", 0},
                {"范围变更时间", "\\N", "2022/04/01", "2023/04/01", "TEXT", 0},
                {"立项通过时间", " ", "2022/02/15", "2023/02/15", "TEXT", 0},
                {"需求立项时间", "null", "2022/02/10", "2023/02/10", "TEXT", 0},
                {"需求提出部门审批日期", "NULL", "2022/02/08", "2023/02/08", "TEXT", 0},
                {"需求首次投产时间", "\\N", "2022/05/21", "2023/05/21", "TEXT", 0},
                {"需求确认日期", "null", "2022/02/09", "2023/02/09", "TEXT", 0},
                {"需求受理日期", "NULL", "2022/02/11", "2023/02/11", "TEXT", 0},
                {"需求分析完成日期", "\\N", "2022/02/20", "2023/02/20", "TEXT", 0},
                {"暂停时长", "null", "0", "5", "LONG", 2},
                {"项目暂停时长", "NULL", 0, 5, "LONG", 2},
                {"需求提出到项目首投产天数", "\\N", "100", "140", "LONG", 2},
                {"需求提出到项目终投产天数", " ", 1774, 140, "LONG", 2},
                {"需求确认到项目首投产天数", "null", "101", "139", "LONG", 2},
                {"需求确认到项目终投产天数", "NULL", "1775", "139", "LONG", 2},
                {"需求受理到首次投产天数", "\\N", "99", "137", "LONG", 2},
                {"需求受理到最终投产天数", " ", 1773, 137, "LONG", 2},
                {"需求分析完成到首次投产天数", "null", "90", "128", "LONG", 2},
                {"需求分析完成到最终投产天数", "NULL", 1764, 128, "LONG", 2},
                {"需求首次投产到需求确认天数", "\\N", -101, -139, "LONG", 2}
        };
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("复杂字段矩阵");
            Row header = sheet.createRow(0);
            Row emptyLikeRow = sheet.createRow(1);
            Row firstDataRow = sheet.createRow(2);
            Row secondDataRow = sheet.createRow(3);
            for (int i = 0; i < fieldCases.length; i++) {
                header.createCell(i).setCellValue((String) fieldCases[i][0]);
                writeCell(emptyLikeRow, i, fieldCases[i][1]);
                writeCell(firstDataRow, i, fieldCases[i][2]);
                writeCell(secondDataRow, i, fieldCases[i][3]);
            }

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertEquals(fieldCases.length, dataSheet.getFields().size());
            assertEquals(3, dataSheet.getJsonArray().size());
            for (Object[] fieldCase : fieldCases) {
                String fieldName = (String) fieldCase[0];
                assertField(dataSheet, fieldName, (String) fieldCase[4], (Integer) fieldCase[5]);
                assertNull(dataSheet.getJsonArray().get(0).get(fieldName), fieldName);
            }
            assertEquals("A20210186", dataSheet.getJsonArray().get(1).get("项目编号"));
            assertEquals("199.00", dataSheet.getJsonArray().get(1).get("开发预估工作量"));
            assertEquals("1774", dataSheet.getJsonArray().get(1).get("需求提出到项目终投产天数"));
            assertEquals("-139", dataSheet.getJsonArray().get(2).get("需求首次投产到需求确认天数"));
        }
    }

    @Test
    void excelImportCoversMessyWideExportWithAllNullableConversionCandidates() throws Exception {
        Object[][] fieldCases = {
                {"项目编号", "null", "000001", "000002", "TEXT", 0},
                {"需求编号", "NULL", "REQ-202605150001", "202605150002", "TEXT", 0},
                {"版本号", "\\N", "202606001", "202606002", "TEXT", 0},
                {"工单ID", " ", "0088", "0099", "TEXT", 0},
                {"dept_id", "null", "0007", "0008", "TEXT", 0},
                {"valid", "", 1, 2, "LONG", 2},
                {"solid", "   ", 3, 4, "LONG", 2},
                {"归属处室", "Null", "开发一室", "数据应用开发室", "TEXT", 0},
                {"项目版本投产时间", "null", "2024/05/27", "2026/12/30", "TEXT", 0},
                {"项目版本提测时间", "NULL", "2024-05-01", "2026-11-30", "TEXT", 0},
                {"项目首次投产日期", "\\N", "2024/05/27", "2026/12/30", "TEXT", 0},
                {"项目最终投产日期", " ", "2024/05/27", "2026/12/31", "TEXT", 0},
                {"验收测试报告提交时间", "null", "2024/06/05 10:20:30", "2026/12/20 09:00:00", "TEXT", 0},
                {"项目暂停时间", "NULL", "2024/04/01", "2026/10/01", "TEXT", 0},
                {"项目恢复时间", "\\N", "2024/04/05", "2026/10/05", "TEXT", 0},
                {"需求确认日期", " ", "2024/01/09", "2026/08/09", "TEXT", 0},
                {"开发预估工作量", "null", 199, 825, "DOUBLE", 3},
                {"架构师评估工作量", "NULL", "174.00", "654.00", "DOUBLE", 3},
                {"平均耗时", "\\N", 18, 20, "DOUBLE", 3},
                {"均值指标", " ", "1", "2", "DOUBLE", 3},
                {"阈值", "null", 30, 30.5, "DOUBLE", 3},
                {"金额", "NULL", 123456, 0, "DOUBLE", 3},
                {"暂停时长", "\\N", "0", "5", "LONG", 2},
                {"项目暂停时长", " ", 0, 5, "LONG", 2},
                {"需求提出到项目首投产天数", "null", "100", "140", "LONG", 2},
                {"需求提出到项目终投产天数", "NULL", 1774, 140, "LONG", 2},
                {"需求首次投产到需求确认天数", "\\N", -101, -139, "LONG", 2},
                {"投产版本数", " ", 3, 4, "LONG", 2},
                {"超期数量", "null", 0, 2, "LONG", 2},
                {"业务审批耗时(天)", "NULL", 21.5, "待确认", "TEXT", 0},
                {"百分比文本", "\\N", "12.5%", "0%", "TEXT", 0},
                {"科学计数文本", " ", "1.23E4", "9.9E-2", "TEXT", 0},
                {"长编码", "null", "123456789012345678901", "ABCD", "TEXT", 0},
                {"空值列", "NULL", null, null, "TEXT", 0},
                {"SQL片段文本", "\\N", "SELECT a FROM t WHERE id = 1", "普通说明", "TEXT", 0},
                {"需求业务审批会签耗时（天）【需求提出-业务部门会签完成后出需求】", " ", 33, 37, "LONG", 2},
                {"重复指标", "null", 7, 8, "LONG", 2},
                {"重复指标", "NULL", "A", "B", "TEXT", 0}
        };
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("复杂导出");
            Row header = sheet.createRow(0);
            Row emptyLikeRow = sheet.createRow(1);
            Row firstDataRow = sheet.createRow(2);
            Row secondDataRow = sheet.createRow(3);
            for (int i = 0; i < fieldCases.length; i++) {
                header.createCell(i).setCellValue((String) fieldCases[i][0]);
                writeCell(emptyLikeRow, i, fieldCases[i][1]);
                writeCell(firstDataRow, i, fieldCases[i][2]);
                writeCell(secondDataRow, i, fieldCases[i][3]);
            }

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertEquals(fieldCases.length, dataSheet.getFields().size());
            assertEquals(3, dataSheet.getJsonArray().size());
            assertEquals("重复指标_2", dataSheet.getFields().get(dataSheet.getFields().size() - 1).getName());
            for (int i = 0; i < fieldCases.length; i++) {
                String fieldName = dataSheet.getFields().get(i).getName();
                assertField(dataSheet, fieldName, (String) fieldCases[i][4], (Integer) fieldCases[i][5]);
                assertNull(dataSheet.getJsonArray().get(0).get(fieldName), fieldName);
                assertTrue(dataSheet.getFields().get(i).getDbFieldName().length() <= 64, fieldName);
            }
            assertEquals("000001", dataSheet.getJsonArray().get(1).get("项目编号"));
            assertEquals("199", dataSheet.getJsonArray().get(1).get("开发预估工作量"));
            assertEquals("1774", dataSheet.getJsonArray().get(1).get("需求提出到项目终投产天数"));
            assertEquals("SELECT a FROM t WHERE id = 1", dataSheet.getJsonArray().get(1).get("SQL片段文本"));
            assertEquals("-139", dataSheet.getJsonArray().get(2).get("需求首次投产到需求确认天数"));
            assertEquals("B", dataSheet.getJsonArray().get(2).get("重复指标_2"));
        }
    }

    @Test
    void csvImportKeepsIdentifierAndDateLikeNumericTextAsText() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("""
                项目编号,需求编号,投产月份,需求数,平均耗时,空值列
                000001,202605150001,202606, 12 , 18.50 ,
                000002,202605150002,202607,0,-3.25,
                """);

        ExcelSheetData dataSheet = sheets.get(0);
        assertField(dataSheet, "项目编号", "TEXT", 0);
        assertField(dataSheet, "需求编号", "TEXT", 0);
        assertField(dataSheet, "投产月份", "TEXT", 0);
        assertField(dataSheet, "需求数", "LONG", 2);
        assertField(dataSheet, "平均耗时", "DOUBLE", 3);
        assertField(dataSheet, "空值列", "TEXT", 0);
        assertEquals("000001", dataSheet.getJsonArray().get(0).get("项目编号"));
        assertEquals(" 12 ", dataSheet.getJsonArray().get(0).get("需求数"));
        assertEquals("", dataSheet.getJsonArray().get(1).get("空值列"));
    }

    @Test
    void csvImportIgnoresWhitespaceOnlyCellsDuringTypeInference() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("""
                需求数,平均耗时,说明
                   ,   ,首行空白
                12,18.5,第二行有效
                0,-3.25,第三行有效
                """);

        ExcelSheetData dataSheet = sheets.get(0);
        assertField(dataSheet, "需求数", "LONG", 2);
        assertField(dataSheet, "平均耗时", "DOUBLE", 3);
        assertField(dataSheet, "说明", "TEXT", 0);
        assertEquals("   ", dataSheet.getJsonArray().get(0).get("需求数"));
        assertEquals("   ", dataSheet.getJsonArray().get(0).get("平均耗时"));
    }

    @Test
    void csvImportTreatsLiteralNullPlaceholdersAsMissingValuesForTypeInference() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("""
                项目编号,需求数,平均耗时,备注
                A001,null,18.5,NULL
                A002,12,null,\\N
                A003,0,-3.25,正常
                """);

        ExcelSheetData dataSheet = sheets.get(0);
        assertField(dataSheet, "项目编号", "TEXT", 0);
        assertField(dataSheet, "需求数", "LONG", 2);
        assertField(dataSheet, "平均耗时", "DOUBLE", 3);
        assertField(dataSheet, "备注", "TEXT", 0);
        assertNull(dataSheet.getJsonArray().get(0).get("需求数"));
        assertNull(dataSheet.getJsonArray().get(0).get("备注"));
        assertNull(dataSheet.getJsonArray().get(1).get("平均耗时"));
        assertNull(dataSheet.getJsonArray().get(1).get("备注"));
        assertEquals("0", dataSheet.getJsonArray().get(2).get("需求数"));
    }

    @Test
    void excelImportDoesNotTreatOrdinaryEnglishWordsEndingWithIdAsIdentifiers() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("英文后缀");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("valid");
            header.createCell(1).setCellValue("solid");
            header.createCell(2).setCellValue("dept_id");
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue(1);
            first.createCell(1).setCellValue(2);
            first.createCell(2).setCellValue("0088");

            ExcelSheetData dataSheet = parseWorkbook(workbook).get(0);

            assertField(dataSheet, "valid", "LONG", 2);
            assertField(dataSheet, "solid", "LONG", 2);
            assertField(dataSheet, "dept_id", "TEXT", 0);
        }
    }

    @Test
    void excelImportSkipsSheetWithBlankHeaderGap() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("空表头");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("月份");
            header.createCell(2).setCellValue("数量");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("2026年1月");
            row.createCell(2).setCellValue(12);

            List<ExcelSheetData> sheets = parseWorkbook(workbook);

            assertEquals(1, sheets.size());
            ExcelSheetData dataSheet = sheets.get(0);
            assertFalse(dataSheet.isSheet());
            assertEquals("SKIPPED", dataSheet.getInspectionStatus());
            assertEquals("检测到空表头，默认不导入。", dataSheet.getInspectionMessage());
            assertTrue(dataSheet.getFields().isEmpty());
            assertTrue(dataSheet.getJsonArray().isEmpty());
        }
    }

    @Test
    void csvImportPreservesQuotedCommasAndMissingCellsAsSafeValues() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("部门,说明,数量,空值\n研发,\"包含,逗号\",12,\n测试,,,\n");

        ExcelSheetData dataSheet = sheets.get(0);
        assertTrue(dataSheet.isSheet());
        assertEquals(4, dataSheet.getFields().size());
        assertField(dataSheet, "部门", "TEXT", 0);
        assertField(dataSheet, "说明", "TEXT", 0);
        assertField(dataSheet, "数量", "LONG", 2);
        assertField(dataSheet, "空值", "TEXT", 0);
        assertEquals("包含,逗号", dataSheet.getJsonArray().get(0).get("说明"));
        assertEquals("", dataSheet.getJsonArray().get(0).get("空值"));
        assertEquals("", dataSheet.getJsonArray().get(1).get("说明"));
        assertEquals("", dataSheet.getJsonArray().get(1).get("数量"));
        assertEquals("", dataSheet.getJsonArray().get(1).get("空值"));
    }

    @Test
    void csvImportParsesQuotedHeadersWithCommas() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("\"部门,名称\",数量\n研发一室,12\n");

        ExcelSheetData dataSheet = sheets.get(0);
        assertTrue(dataSheet.isSheet());
        assertEquals(2, dataSheet.getFields().size());
        assertField(dataSheet, "部门,名称", "TEXT", 0);
        assertField(dataSheet, "数量", "LONG", 2);
        assertEquals("研发一室", dataSheet.getJsonArray().get(0).get("部门,名称"));
        assertEquals("12", dataSheet.getJsonArray().get(0).get("数量"));
    }

    @Test
    void excelGetTableFieldsNormalizesLegacyFieldsWithoutType() throws Exception {
        ExcelSheetData sheet = new ExcelSheetData();
        sheet.setSheet(true);
        sheet.setDisplayTableName("excel_legacy");
        sheet.setTableName("legacy");
        TableField field = checkedTextField("数量", "f_qty");
        field.setType(null);
        field.setNativeType("LONG");
        field.setFieldType(null);
        field.setExtractedFieldType(null);
        sheet.setFields(java.util.List.of(field));

        DatasourceRequest request = new DatasourceRequest();
        DatasourceDTO datasource = new DatasourceDTO();
        datasource.setType("Excel");
        datasource.setConfiguration(JsonUtil.toJSONString(java.util.List.of(sheet)).toString());
        request.setDatasource(datasource);
        request.setTable("excel_legacy");

        List<TableField> fields = ExcelUtils.getTableFields(request);

        assertEquals(1, fields.size());
        assertEquals("LONG", fields.get(0).getType());
        assertEquals("LONG", fields.get(0).getNativeType());
        assertEquals(2, fields.get(0).getFieldType());
        assertEquals(2, fields.get(0).getExtractedFieldType());
    }

    @Test
    void excelInsertUsesBindParametersForImportedCellValues() {
        MysqlEngineProvider provider = new MysqlEngineProvider();
        TableField note = checkedTextField("说明", "f_note");
        TableField skipped = checkedTextField("跳过", "f_skip");
        skipped.setChecked(false);
        TableField quantity = checkedTextField("数量", "f_qty");
        List<TableField> fields = List.of(note, skipped, quantity);
        List<String[]> rows = List.of(
                new String[]{"x'); DROP TABLE users; --", "ignored", "12"},
                new String[]{"", "ignored"}
        );

        String sql = provider.insertSql("Excel", "excel_sheet", DatasourceServer.UpdateType.all_scope, rows, 1, 1000, fields);
        List<TableFieldWithValue> values = provider.insertParams("Excel", "excel_sheet", DatasourceServer.UpdateType.all_scope, rows, 1, 1000, fields);
        String h2Sql = new H2EngineProvider().insertSql("Excel", "excel_sheet", DatasourceServer.UpdateType.all_scope, rows, 1, 1000, fields);

        assertEquals("INSERT INTO `tmp_excel_sheet` VALUES (?,?),(?,?)", sql);
        assertEquals("INSERT INTO  \"tmp_excel_sheet\" VALUES (?,?),(?,?)", h2Sql);
        assertFalse(sql.contains("DROP TABLE"));
        assertFalse(h2Sql.contains("DROP TABLE"));
        assertEquals("x'); DROP TABLE users; --", values.get(0).getValue());
        assertEquals("12", values.get(1).getValue());
        assertNull(values.get(2).getValue());
        assertNull(values.get(3).getValue());
    }

    @Test
    void mysqlEngineRejectsIdentifierDelimitersInGeneratedSql() {
        TableField field = checkedTextField("字段", "f`name");

        assertThrows(CrestException.class, () -> new MysqlEngineProvider().createTableSql("excel_sheet", List.of(field), engine("mysql")));
        assertThrows(CrestException.class, () -> new MysqlEngineProvider().createTableSql("excel`sheet", List.of(checkedTextField("字段", "f_name")), engine("mysql")));
    }

    @Test
    void mysqlEngineQuotesValidIdentifiersInGeneratedSql() {
        TableField field = checkedTextField("字段", "f_name");

        String createSql = new MysqlEngineProvider().createTableSql("excel_sheet", List.of(field), engine("mysql"));
        String insertSql = new MysqlEngineProvider().insertSql("Excel", "excel_sheet", DatasourceServer.UpdateType.all_scope,
                List.<String[]>of(new String[]{"1"}), 1, 1000, List.of(field));

        assertTrue(createSql.contains("`excel_sheet`"));
        assertTrue(createSql.contains("`f_name` longtext"));
        assertEquals("INSERT INTO `tmp_excel_sheet` VALUES (?)", insertSql);
    }

    @Test
    void h2EngineRejectsIdentifierDelimitersInGeneratedSql() {
        TableField field = checkedTextField("字段", "f\"name");

        assertThrows(CrestException.class, () -> new H2EngineProvider().createTableSql("excel_sheet", List.of(field), engine("h2")));
        assertThrows(CrestException.class, () -> new H2EngineProvider().createTableSql("excel\"sheet", List.of(checkedTextField("字段", "f_name")), engine("h2")));
    }

    @Test
    void h2EngineQuotesValidIdentifiersInGeneratedSql() {
        TableField field = checkedTextField("字段", "f_name");

        String createSql = new H2EngineProvider().createTableSql("excel_sheet", List.of(field), engine("h2"));
        String insertSql = new H2EngineProvider().insertSql("Excel", "excel_sheet", DatasourceServer.UpdateType.all_scope,
                List.<String[]>of(new String[]{"1"}), 1, 1000, List.of(field));

        assertTrue(createSql.contains("\"excel_sheet\""));
        assertTrue(createSql.contains("\"f_name\" longtext"));
        assertEquals("INSERT INTO  \"tmp_excel_sheet\" VALUES (?)", insertSql);
    }

    @Test
    void engineCreateTableRejectsUnsafeColumnLength() {
        TableField field = checkedTextField("字段", "f_name");
        field.setLength("20); DROP TABLE users; --");

        assertThrows(CrestException.class, () -> new MysqlEngineProvider().createTableSql("excel_sheet", List.of(field), engine("mysql")));
        assertThrows(CrestException.class, () -> new H2EngineProvider().createTableSql("excel_sheet", List.of(field), engine("h2")));
    }

    @Test
    void engineCreateTableAllowsBoundedNumericColumnLength() {
        TableField mysqlField = checkedTextField("字段", "f_name");
        mysqlField.setLength("64");
        TableField h2Field = checkedTextField("字段", "f_name");
        h2Field.setLength("64");

        String mysqlSql = new MysqlEngineProvider().createTableSql("excel_sheet", List.of(mysqlField), engine("mysql"));
        String h2Sql = new H2EngineProvider().createTableSql("excel_sheet", List.of(h2Field), engine("h2"));

        assertTrue(mysqlSql.contains("`f_name` varchar(64)"));
        assertTrue(h2Sql.contains("\"f_name\" varchar(64)"));
    }

    @Test
    void engineCreateViewRejectsNonReadOnlySql() {
        assertThrows(CrestException.class, () -> new MysqlEngineProvider().createView("v_safe", "select 1; drop table users"));
        assertThrows(CrestException.class, () -> new H2EngineProvider().createView("v_safe", "delete from users"));
    }

    @Test
    void csvImportSkipsSheetWithBlankHeaderCell() throws Exception {
        List<ExcelSheetData> sheets = parseCsv("月份,   ,数量\n2026年1月,12,24\n");

        assertEquals(1, sheets.size());
        ExcelSheetData dataSheet = sheets.get(0);
        assertFalse(dataSheet.isSheet());
        assertEquals("SKIPPED", dataSheet.getInspectionStatus());
        assertEquals("检测到空表头，默认不导入。", dataSheet.getInspectionMessage());
        assertTrue(dataSheet.getFields().isEmpty());
        assertTrue(dataSheet.getJsonArray().isEmpty());
    }

    @Test
    void excelTablesExposeOnlyImportableSheets() throws Exception {
        List<ExcelSheetData> sheets = parseWorkbook(workbookWithSqlDescriptionSheet());
        fillSavedSheetMetadata(sheets);
        DatasourceRequest request = new DatasourceRequest();
        DatasourceDTO datasource = new DatasourceDTO();
        datasource.setId(1L);
        datasource.setType("Excel");
        datasource.setConfiguration(JsonUtil.toJSONString(sheets).toString());
        datasource.setCreateTime(1000L);
        request.setDatasource(datasource);

        List<DatasetTableDTO> tables = ExcelUtils.tables(request);

        assertEquals(1, tables.size());
        assertEquals("excel_数据", tables.get(0).getTableName());
    }

    private void assertField(ExcelSheetData sheet, String name, String type, int fieldType) {
        TableField field = sheet.getFields().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(type, field.getType());
        assertEquals(type, field.getNativeType());
        assertEquals(fieldType, field.getFieldType());
        assertEquals(fieldType, field.getExtractedFieldType());
    }

    private TableField checkedTextField(String name, String dbFieldName) {
        TableField field = new TableField();
        field.setName(name);
        field.setOriginName(name);
        field.setDbFieldName(dbFieldName);
        field.setNativeType("TEXT");
        field.setFieldType(0);
        field.setExtractedFieldType(0);
        field.setChecked(true);
        return field;
    }

    private CoreEngine engine(String type) {
        CoreEngine engine = new CoreEngine();
        engine.setType(type);
        return engine;
    }

    private Workbook workbookWithSqlDescriptionSheet() {
        Workbook workbook = new XSSFWorkbook();
        Sheet description = workbook.createSheet("说明");
        description.createRow(0).createCell(0).setCellValue(
                "SELECT distinct a.code as 项目编号,bb.name as 项目类型,t.name as 项目级别,ad.displayname FROM project_table a LEFT JOIN project_type bb ON a.type_id = bb.id WHERE a.deleted = 0"
        );
        Sheet data = workbook.createSheet("数据");
        Row header = data.createRow(0);
        header.createCell(0).setCellValue("月份");
        header.createCell(1).setCellValue("数量");
        Row row = data.createRow(1);
        row.createCell(0).setCellValue("2026年1月");
        row.createCell(1).setCellValue(12);
        return workbook;
    }

    private void fillSavedSheetMetadata(List<ExcelSheetData> sheets) {
        for (ExcelSheetData sheet : sheets) {
            sheet.setTableName(sheet.getExcelLabel());
            sheet.setDisplayTableName("excel_" + sheet.getExcelLabel());
            sheet.setLastUpdateTime(1000L);
            sheet.setSheetId(UUID.randomUUID().toString());
            sheet.setSheetExcelId(UUID.randomUUID().toString());
            sheet.setFileName("import.xlsx");
            sheet.setPath("/tmp/import.xlsx");
        }
    }

    @SuppressWarnings("unchecked")
    private List<ExcelSheetData> parseWorkbook(Workbook workbook) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        ExcelUtils excelUtils = new ExcelUtils();
        Method parseExcel = ExcelUtils.class.getDeclaredMethod(
                "parseExcel",
                String.class,
                java.io.InputStream.class,
                boolean.class,
                String.class
        );
        parseExcel.setAccessible(true);
        return (List<ExcelSheetData>) parseExcel.invoke(
                excelUtils,
                "import.xlsx",
                new ByteArrayInputStream(outputStream.toByteArray()),
                true,
                "import.xlsx"
        );
    }

    @SuppressWarnings("unchecked")
    private List<ExcelSheetData> parseCsv(String content) throws Exception {
        ExcelUtils excelUtils = new ExcelUtils();
        Method parseExcel = ExcelUtils.class.getDeclaredMethod(
                "parseExcel",
                String.class,
                java.io.InputStream.class,
                boolean.class,
                String.class
        );
        parseExcel.setAccessible(true);
        return (List<ExcelSheetData>) parseExcel.invoke(
                excelUtils,
                "import.csv",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                true,
                "import.csv"
        );
    }

    private void writeCell(Row row, int column, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number) {
            row.createCell(column).setCellValue(((Number) value).doubleValue());
            return;
        }
        if (value instanceof Boolean) {
            row.createCell(column).setCellValue((Boolean) value);
            return;
        }
        row.createCell(column).setCellValue(value.toString());
    }
}
