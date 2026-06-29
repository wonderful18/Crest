package io.crest.chart;

import io.crest.chart.server.ChartDataServer;
import io.crest.api.chart.request.ChartExcelRequest;
import io.crest.constant.FieldTypeConstants;
import io.crest.engine.constant.ExtFieldConstant;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.FormatterCfgDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartDataServerExportModeTest {

    @Test
    void publicLinkPopupExportShouldReturnFileStreamDirectly() {
        assertTrue(ChartDataServer.shouldWriteDirectExport("link-token", true));
    }

    @Test
    void publicLinkExportShouldReturnFileStreamDirectly() {
        assertTrue(ChartDataServer.shouldWriteDirectExport("link-token", false));
    }

    @Test
    void embeddedExportShouldReturnFileStreamDirectly() {
        assertTrue(ChartDataServer.shouldWriteDirectExport("", true));
    }

    @Test
    void normalExportShouldUseExportCenter() {
        assertFalse(ChartDataServer.shouldWriteDirectExport("", false));
    }

    @Test
    void tableExportShouldForcePageModeBeforeQueryingTotalPages() {
        ChartExcelRequest request = new ChartExcelRequest();
        ChartViewDTO viewInfo = new ChartViewDTO();
        Map<String, Object> basicStyle = new HashMap<>();
        basicStyle.put("tablePageMode", "pull");
        Map<String, Object> customAttr = new HashMap<>();
        customAttr.put("basicStyle", basicStyle);
        viewInfo.setType("table-info");
        viewInfo.setCustomAttr(customAttr);
        request.setViewInfo(viewInfo);

        ChartDataServer.prepareTableViewForExport(request);

        assertEquals("page", basicStyle.get("tablePageMode"));
    }

    @Test
    void formulaSummaryShouldOverrideBuiltInAverageOnExport() {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setEngineFieldName("f_profit");
        field.setField("f_profit");
        String fieldName = field.getEngineFieldName();
        ChartDataServer.SummaryConfig config = new ChartDataServer.SummaryConfig();
        config.summaryTypeMap.put(fieldName, "avg");
        config.summaryShowMap.put(fieldName, true);

        ChartDataServer.SummaryAccumulator acc = new ChartDataServer.SummaryAccumulator();
        acc.sumMap.put(fieldName, new BigDecimal("10"));
        acc.countMap.put(fieldName, 2L);

        Object[] row = ChartDataServer.buildSummaryRow(
                List.of(field),
                config,
                acc,
                Map.of(fieldName, new BigDecimal("88.8")));

        assertEquals("88.8", row[0]);
    }

    @Test
    void summaryColumnsShouldAppendVirtualExportColumnsAndSummaryValues() {
        ChartViewDTO viewInfo = new ChartViewDTO();
        viewInfo.setType("table-info");
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setEngineFieldName("f_project");
        field.setField("f_project");
        viewInfo.setXAxis(List.of(field));
        viewInfo.setYAxis(List.of());
        viewInfo.setXAxisExt(List.of());
        viewInfo.setYAxisExt(List.of());
        viewInfo.setExtStack(List.of());

        Map<String, Object> basicStyle = new HashMap<>();
        basicStyle.put("showSummary", true);
        basicStyle.put("summaryLabel", "总计");
        basicStyle.put("summaryColumns", List.of(
                Map.of(
                        "id", "summary_avg_duration",
                        "name", "平均处理时长",
                        "show", true,
                        "summary", "custom",
                        "field", "f_duration",
                        "originName", "SUM([duration]) / SUM([count])",
                        "format", Map.of("emptyText", "--")),
                Map.of(
                        "id", "summary_hidden",
                        "name", "隐藏汇总",
                        "show", false,
                        "summary", "sum")));
        viewInfo.setCustomAttr(Map.of("basicStyle", basicStyle));

        ChartDataServer.SummaryConfig config = ChartDataServer.parseSummaryConfig(viewInfo);
        List<ChartViewFieldDTO> columns = ChartDataServer.getAllExportColumns(viewInfo);

        assertEquals(1, config.summaryColumns.size());
        assertEquals(2, columns.size());
        assertEquals("summary_avg_duration", columns.get(1).getEngineFieldName());
        assertEquals("平均处理时长", columns.get(1).getName());

        Object[] row = ChartDataServer.buildSummaryRow(
                columns,
                config,
                new ChartDataServer.SummaryAccumulator(),
                Map.<String, BigDecimal>of(),
                Map.of("summary_avg_duration", new BigDecimal("6.50")));

        assertEquals("6.50", row[1]);
    }

    @Test
    void oldSummaryConfigWithoutSummaryColumnsShouldKeepExportColumns() {
        ChartViewDTO viewInfo = new ChartViewDTO();
        viewInfo.setType("table-info");
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setEngineFieldName("f_project");
        field.setField("f_project");
        viewInfo.setXAxis(List.of(field));
        viewInfo.setCustomAttr(Map.of("basicStyle", Map.of(
                "showSummary", true,
                "summaryLabel", "总计")));

        ChartDataServer.SummaryConfig config = ChartDataServer.parseSummaryConfig(viewInfo);
        List<ChartViewFieldDTO> columns = ChartDataServer.getAllExportColumns(viewInfo);

        assertTrue(config.summaryColumns.isEmpty());
        assertEquals(1, columns.size());
        assertEquals("f_project", columns.get(0).getEngineFieldName());
    }

    @Test
    void summaryColumnConfigShouldAcceptStringBooleansAndFormats() {
        ChartViewDTO viewInfo = new ChartViewDTO();
        viewInfo.setType("table-normal");
        viewInfo.setCustomAttr(Map.of("basicStyle", Map.of(
                "showSummary", "true",
                "summaryLabel", "总计",
                "summaryColumns", List.of(
                        Map.of(
                                "id", "summary_rate",
                                "name", "完成率",
                                "show", "true",
                                "summary", "custom",
                                "originName", "SUM([done]) / SUM([total])",
                                "format", Map.of(
                                        "percent", "true",
                                        "decimal", 1,
                                        "thousandSeparator", false,
                                        "unit", "%")),
                        Map.of(
                                "id", "summary_hidden",
                                "name", "隐藏列",
                                "show", "false")))));

        ChartDataServer.SummaryConfig config = ChartDataServer.parseSummaryConfig(viewInfo);

        assertTrue(ChartDataServer.isSummaryEnabled(viewInfo));
        assertEquals(1, config.summaryColumns.size());
        assertEquals("percent", config.summaryColumns.get(0).formatterCfg().getType());
        assertEquals(1, config.summaryColumns.get(0).formatterCfg().getDecimalCount());
        assertFalse(config.summaryColumns.get(0).formatterCfg().getThousandSeparator());
    }

    @Test
    void appendSummaryColumnPlaceholdersShouldPadDetailRowsForExport() {
        ChartDataServer.SummaryConfig config = new ChartDataServer.SummaryConfig();
        config.summaryColumns.add(new ChartDataServer.SummaryColumnConfig(
                "summary_avg_duration",
                "平均处理时长",
                true,
                "custom",
                "f_duration",
                "",
                "--",
                new FormatterCfgDTO()));
        List<Object[]> details = new java.util.ArrayList<>();
        details.add(new Object[]{"需求A", 3});

        ChartDataServer.appendSummaryColumnPlaceholders(details, config);

        assertEquals(3, details.get(0).length);
        assertEquals("--", details.get(0)[2]);
    }

    @Test
    void summaryColumnCalcFieldsShouldKeepVirtualResultIdAndRealMetricSource() {
        ChartViewDTO viewInfo = new ChartViewDTO();
        viewInfo.setType("table-normal");
        Map<String, Object> basicStyle = new HashMap<>();
        basicStyle.put("showSummary", true);
        basicStyle.put("summaryLabel", "总计");
        basicStyle.put("summaryColumns", List.of(
                Map.of(
                        "id", "summary_amount_sum",
                        "name", "总金额",
                        "show", true,
                        "summary", "sum",
                        "field", "f_amount"),
                Map.of(
                        "id", "summary_duration_avg",
                        "name", "平均处理时长",
                        "show", true,
                        "summary", "avg",
                        "field", "f_duration"),
                Map.of(
                        "id", "summary_rate",
                        "name", "完成率",
                        "show", true,
                        "summary", "custom",
                        "originName", "SUM([done]) / SUM([total])",
                        "format", Map.of(
                                "decimal", 2,
                                "percent", true,
                                "thousandSeparator", true,
                                "unit", "")),
                Map.of(
                        "id", "summary_count",
                        "name", "数量",
                        "show", true,
                        "summary", "count",
                        "field", "f_duration"),
                Map.of(
                        "id", "summary_deleted",
                        "name", "已删除字段",
                        "show", true,
                        "summary", "sum",
                        "field", "f_deleted")));
        viewInfo.setCustomAttr(Map.of("basicStyle", basicStyle));
        DatasetTableFieldDTO amount = new DatasetTableFieldDTO();
        amount.setEngineFieldName("f_amount");
        amount.setOriginName("amount");
        amount.setName("金额");
        amount.setFieldType(FieldTypeConstants.FLOAT);
        amount.setExtField(0);
        DatasetTableFieldDTO duration = new DatasetTableFieldDTO();
        duration.setEngineFieldName("f_duration");
        duration.setOriginName("duration");
        duration.setName("处理时长");
        duration.setFieldType(FieldTypeConstants.FLOAT);
        duration.setExtField(0);

        List<ChartViewFieldDTO> fields = ChartDataServer.buildSummaryColumnCalcFields(viewInfo, List.of(amount, duration));

        assertEquals(4, fields.size());
        assertEquals("summary_amount_sum", fields.get(0).getField());
        assertEquals("amount", fields.get(0).getOriginName());
        assertEquals("sum", fields.get(0).getSummary());
        assertEquals("summary_duration_avg", fields.get(1).getField());
        assertEquals("duration", fields.get(1).getOriginName());
        assertEquals("avg", fields.get(1).getSummary());
        assertEquals("summary_rate", fields.get(2).getField());
        assertEquals("SUM([done]) / SUM([total])", fields.get(2).getOriginName());
        assertEquals(ExtFieldConstant.EXT_CALC, fields.get(2).getExtField());
        assertEquals("percent", fields.get(2).getFormatterCfg().getType());
        assertEquals(2, fields.get(2).getFormatterCfg().getDecimalCount());
        assertEquals("summary_count", fields.get(3).getField());
        assertEquals("count", fields.get(3).getSummary());
    }
}
