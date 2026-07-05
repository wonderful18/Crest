package io.crest.chart.utils;

import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartDataBuildTest {

    @Test
    void transBarRangeDataAntVSkipsRowsWithoutRangeValues() {
        ChartViewFieldDTO dimension = field(1L, "项目", null, null);
        ChartViewFieldDTO start = field(2L, "开始日期", "y_M_d", "date_sub");
        ChartViewFieldDTO end = field(3L, "结束日期", "y_M_d", "date_sub");
        ChartViewDTO view = new ChartViewDTO();
        view.setAggregate(false);

        Map<String, Object> result = assertDoesNotThrow(() -> ChartDataBuild.transBarRangeDataAntV(
                false,
                true,
                List.of(dimension),
                List.of(dimension, start, end),
                List.of(),
                view,
                List.<String[]>of(new String[]{"需求A"}),
                false
        ));

        assertTrue(((List<?>) result.get("data")).isEmpty());
    }

    @Test
    void transBarRangeDataAntVKeepsValidDateRangeRows() {
        ChartViewFieldDTO dimension = field(1L, "项目", null, null);
        ChartViewFieldDTO start = field(2L, "开始日期", "y_M_d", "date_sub");
        ChartViewFieldDTO end = field(3L, "结束日期", "y_M_d", "date_sub");
        ChartViewDTO view = new ChartViewDTO();
        view.setAggregate(false);

        Map<String, Object> result = ChartDataBuild.transBarRangeDataAntV(
                false,
                true,
                List.of(dimension),
                List.of(dimension, start, end),
                List.of(),
                view,
                List.<String[]>of(new String[]{"需求A", "2026-06-01", "2026-06-05"}),
                false
        );

        assertEquals(1, ((List<?>) result.get("data")).size());
        assertEquals("2026-06-01", result.get("minTime"));
        assertEquals("2026-06-05", result.get("maxTime"));
    }

    private static ChartViewFieldDTO field(Long id, String name, String dateStyle, String datePattern) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(id);
        field.setName(name);
        field.setDateStyle(dateStyle);
        field.setDatePattern(datePattern);
        return field;
    }
}
