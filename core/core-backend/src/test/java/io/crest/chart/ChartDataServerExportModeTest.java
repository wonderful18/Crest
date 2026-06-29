package io.crest.chart;

import io.crest.chart.server.ChartDataServer;
import io.crest.api.chart.request.ChartExcelRequest;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
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
}
