package io.crest.chart;

import io.crest.chart.server.ChartDataServer;
import io.crest.constant.FieldTypeConstants;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartDataServerAuxiliaryHeaderTest {

    @Test
    void tableExportShouldWriteHeaderDescriptionBeforeDataRows() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("数据");
            CellStyle headerStyle = workbook.createCellStyle();
            Object[] header = {"月份", "耗时"};
            List<Object[]> details = new ArrayList<>();
            details.add(header);
            details.add(new Object[]{"2026年1月", 9});

            ChartDataServer.setExcelData(
                    sheet,
                    headerStyle,
                    header,
                    details,
                    null,
                    new Integer[]{FieldTypeConstants.STRING, FieldTypeConstants.STRING},
                    viewInfo(),
                    workbook
            );

            DataFormatter formatter = new DataFormatter();
            assertEquals("月份", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("耗时", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("阶段说明", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("需求发起提出至获得审批的周期 T1", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("2026年1月", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("9", formatter.formatCellValue(sheet.getRow(2).getCell(1)));
        }
    }

    private ChartViewDTO viewInfo() {
        ChartViewDTO viewInfo = new ChartViewDTO();
        viewInfo.setType("table-info");
        viewInfo.setDownloadType("chart");
        viewInfo.setXAxis(List.of(field("month", "月份", FieldTypeConstants.STRING), field("duration", "耗时", FieldTypeConstants.STRING)));
        viewInfo.setYAxis(Collections.emptyList());
        viewInfo.setXAxisExt(Collections.emptyList());
        viewInfo.setYAxisExt(Collections.emptyList());
        viewInfo.setExtStack(Collections.emptyList());
        viewInfo.setDrillFields(Collections.emptyList());
        viewInfo.setCustomAttr(Map.of(
                "basicStyle", Map.of("showSummary", false),
                "tableCell", Map.of("mergeCells", false),
                "tableHeader", Map.of(
                        "auxiliaryHeader", Map.of(
                                "enabled", true,
                                "rowHeight", 120,
                                "fontSize", 14,
                                "align", "center",
                                "descriptions", List.of(
                                        Map.of("field", "month", "text", "阶段说明"),
                                        Map.of("field", "duration", "text", "需求发起提出至获得审批的周期 T1")
                                )
                        )
                )
        ));
        viewInfo.setSenior(Map.of("functionCfg", Map.of()));
        return viewInfo;
    }

    private ChartViewFieldDTO field(String engineFieldName, String name, Integer fieldType) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setEngineFieldName(engineFieldName);
        field.setName(name);
        field.setFieldType(fieldType);
        field.setHide(false);
        return field;
    }
}
