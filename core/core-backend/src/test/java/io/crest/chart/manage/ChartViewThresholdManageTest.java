package io.crest.chart.manage;

import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.view.filter.FilterTreeItem;
import io.crest.extensions.view.filter.FilterTreeObj;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void convertTreeSkipsMissingFieldsFromStaleThresholdRules() {
        ChartViewThresholdManage manage = new ChartViewThresholdManage();
        FilterTreeObj tree = new FilterTreeObj();
        tree.setLogic("and");
        tree.setItems(List.of(item(10L, "lost", "1"), item(20L, "eq", "1")));

        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setName("工时");
        field.setFieldType(0);

        String label = ReflectionTestUtils.invokeMethod(manage, "convertTree", tree, Map.of("20", field));

        assertEquals("工时 i18n_threshold_logic_eq 1", label);
    }

    @Test
    void convertTreeReturnsNullWhenAllFieldsAreMissing() {
        ChartViewThresholdManage manage = new ChartViewThresholdManage();
        FilterTreeObj tree = new FilterTreeObj();
        tree.setLogic("and");
        tree.setItems(List.of(item(10L, "eq", "1")));

        String label = ReflectionTestUtils.invokeMethod(manage, "convertTree", tree, Map.of());

        assertNull(label);
    }

    private FilterTreeItem item(Long fieldId, String term, String value) {
        FilterTreeItem item = new FilterTreeItem();
        item.setType("item");
        item.setFieldId(fieldId);
        item.setFilterType("logic");
        item.setTerm(term);
        item.setValue(value);
        item.setValueType("fixed");
        return item;
    }
}
