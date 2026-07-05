package io.crest.chart.manage;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChartViewThresholdManageTest {

    @Test
    void formatDynamicTimeLabelHandlesCustomYearUnitWithoutNullUnitLabels() {
        ChartViewThresholdManage manage = new ChartViewThresholdManage();

        String label = assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(manage, "formatDynamicTimeLabel",
                "{\"format\":\"YYYY\",\"timeFlag\":9,\"count\":1,\"unit\":1,\"suffix\":1,\"time\":\"00:00:00\"}"));

        assertEquals("1 i18n_time_yeari18n_time_ago", label);
    }

    @Test
    void dynamicTimeUnitLabelsRejectsUnsupportedPureTimeCustomRange() {
        ChartViewThresholdManage manage = new ChartViewThresholdManage();

        assertThrows(CrestException.class, () -> ReflectionTestUtils.invokeMethod(manage, "dynamicTimeUnitLabels", "HH:mm:ss"));
    }
}
