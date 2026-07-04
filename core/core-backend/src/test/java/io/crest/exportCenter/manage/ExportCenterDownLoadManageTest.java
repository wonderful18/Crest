package io.crest.exportCenter.manage;

import io.crest.api.chart.dto.ChartSortFieldDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportCenterDownLoadManageTest {

    @Test
    void buildDatasetSortFieldsUsesCheckedFieldsOnly() {
        DatasetTableFieldDTO checked = field(1L, Boolean.TRUE);
        DatasetTableFieldDTO unchecked = field(2L, Boolean.FALSE);
        DatasetTableFieldDTO legacy = field(3L, null);

        List<ChartSortFieldDTO> result = ExportCenterDownLoadManage.buildDatasetSortFields(
                List.of(checked, unchecked, legacy)
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
        assertEquals("asc", result.getFirst().getOrderDirection());
    }

    private DatasetTableFieldDTO field(Long id, Boolean orderChecked) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(id);
        field.setName("field-" + id);
        field.setEngineFieldName("f_" + id);
        field.setOrderChecked(orderChecked);
        return field;
    }
}
