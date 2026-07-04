package io.crest.chart.charts.impl;

import io.crest.extensions.view.dto.ChartSeniorAssistDTO;
import io.crest.extensions.view.dto.ChartSeniorThresholdDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultChartHandlerTest {

    @Test
    void checkCalcTypeRejectsBlankDateStyle() throws Exception {
        DefaultChartHandler handler = new DefaultChartHandler();
        Method method = DefaultChartHandler.class.getDeclaredMethod("checkCalcType", String.class, String.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(handler, null, "year_mom"));
    }

    @Test
    void conditionFieldsIgnoreMissingDynamicFields() throws Exception {
        DefaultChartHandler handler = new DefaultChartHandler();
        ChartSeniorThresholdDTO condition = new ChartSeniorThresholdDTO();
        condition.setTerm("between");

        Method method = DefaultChartHandler.class.getDeclaredMethod("getConditionFields", ChartSeniorThresholdDTO.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ChartSeniorAssistDTO> fields = (List<ChartSeniorAssistDTO>) method.invoke(handler, condition);

        assertTrue(fields.isEmpty());
    }
}
