package io.crest.visualization.server;

import io.crest.api.visualization.vo.VisualizationViewTableVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisualizationLinkJumpServiceTest {

    @Test
    void componentViewIdsReadsVisibleChartsFromNestedCanvasData() {
        String componentData = """
                [
                  {"component":"UserView","innerType":"bar","id":"12"},
                  {"component":"UserView","innerType":"VQuery","id":"13"},
                  {"component":"Tabs","propValue":[{"componentData":[{"component":"UserView","id":"112"}]}]},
                  {"component":"Group","propValue":[{"component":"UserView","id":"212"}]}
                ]
                """;

        assertEquals(Set.of("12", "112", "212"), VisualizationLinkJumpService.componentViewIds(componentData));
    }

    @Test
    void filterViewTablesDoesNotMatchIdSubstrings() {
        VisualizationViewTableVO view12 = view(12L);
        VisualizationViewTableVO view112 = view(112L);
        String componentData = """
                [{"component":"UserView","id":"112"}]
                """;

        assertEquals(List.of(view112), VisualizationLinkJumpService.filterViewTables(componentData, List.of(view12, view112)));
    }

    private static VisualizationViewTableVO view(Long id) {
        VisualizationViewTableVO view = new VisualizationViewTableVO();
        view.setId(id);
        return view;
    }
}
