package io.crest.chart.charts.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultChartHandlerTest {

    @Test
    void checkCalcTypeRejectsBlankDateStyle() throws ReflectiveOperationException {
        DefaultChartHandler handler = new DefaultChartHandler();
        Method method = DefaultChartHandler.class.getDeclaredMethod("checkCalcType", String.class, String.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(handler, null, "year_mom"));
    }
}
