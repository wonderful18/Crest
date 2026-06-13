package io.crest.datasource.provider;

import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.api.ds.vo.ExcelSheetData;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.utils.JsonUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
