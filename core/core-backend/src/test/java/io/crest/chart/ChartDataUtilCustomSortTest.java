package io.crest.chart;

import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.util.ChartDataUtil;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ChartDataUtilCustomSortTest {

    @Test
    void resultCustomSortSupportsCalculatedMeasureFields() {
        ChartViewFieldDTO dimension = field(1L, "d", "none", List.of());
        ChartViewFieldDTO calculatedMeasure = field(2L, "q", "custom_sort", List.of("B", "A"));
        List<String[]> data = List.of(
                new String[]{"dept", "A"},
                new String[]{"dept", "B"},
                new String[]{"dept", "C"}
        );

        List<String[]> result = ChartDataUtil.resultCustomSort(
                List.of(dimension, calculatedMeasure),
                Collections.emptyList(),
                Collections.emptyList(),
                data
        );

        assertArrayEquals(new String[]{"dept", "B"}, result.get(0));
        assertArrayEquals(new String[]{"dept", "A"}, result.get(1));
        assertArrayEquals(new String[]{"dept", "C"}, result.get(2));
    }

    private ChartViewFieldDTO field(Long id, String groupType, String sort, List<String> customSort) {
        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(id);
        field.setGroupType(groupType);
        field.setSort(sort);
        field.setCustomSort(customSort);
        return field;
    }
}
