package io.crest.dataset.manage;

import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetSQLManageTest {

    @Test
    void inheritEngineFieldNameDoesNotTreatNullIdsAsSameField() {
        DatasetTableFieldDTO first = excelField("业务编号", "f_business_code");
        DatasetTableFieldDTO last = excelField("周期统计字段", "f_cycle_days");
        DatasetTableFieldDTO selected = excelField("业务编号", "f_business_code");

        DatasetSQLManage.inheritEngineFieldName(List.of(first, last), selected);

        assertEquals("f_business_code", selected.getEngineFieldName());
        assertEquals("f_business_code", selected.getFieldShortName());
    }

    @Test
    void inheritEngineFieldNameMatchesPersistedFieldsByIdWhenPresent() {
        DatasetTableFieldDTO persisted = excelField("业务编号", "f_business_code");
        persisted.setId(1001L);
        DatasetTableFieldDTO selected = excelField("业务编号", null);
        selected.setId(1001L);

        DatasetSQLManage.inheritEngineFieldName(List.of(persisted), selected);

        assertEquals("f_business_code", selected.getEngineFieldName());
    }

    private DatasetTableFieldDTO excelField(String name, String physicalName) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setName(name);
        field.setOriginName(name);
        field.setDbFieldName(physicalName);
        field.setEngineFieldName(physicalName);
        field.setFieldShortName(physicalName);
        field.setExtField(0);
        field.setChecked(true);
        return field;
    }
}
