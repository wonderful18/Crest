package io.crest.engine.trans;

import io.crest.constant.FieldTypeConstants;
import io.crest.engine.constant.ExtFieldConstant;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证明细表 custom_sort 下推为 SQL ORDER BY CASE WHEN，
 * 避免分页"先 LIMIT 再内存排序"导致的跨页乱序。
 */
class Dimension2SQLObjTest {

    @Test
    void customSortDimensionShouldProduceCaseWhenOrder() {
        SQLMeta meta = new SQLMeta();
        SQLObj tableObj = SQLObj.builder()
                .tableName("customsort_test")
                .tableAlias("t_a_0")
                .build();
        meta.setTable(tableObj);

        ChartViewFieldDTO dept = new ChartViewFieldDTO();
        dept.setId(1L);
        dept.setOriginName("department");
        dept.setEngineFieldName("f_dept");
        dept.setName("开发处室");
        dept.setGroupType("d");
        dept.setFieldType(FieldTypeConstants.STRING);
        dept.setExtField(ExtFieldConstant.EXT_NORMAL);
        dept.setSort("custom_sort");
        dept.setCustomSort(List.of("开发一室", "开发二室", "开发三室", "平台研发室", "数据应用室"));

        List<ChartViewFieldDTO> fields = new ArrayList<>(List.of(dept));
        List<DatasetTableFieldDTO> originFields = new ArrayList<>();
        originFields.add(copyBase(dept));

        Map<Long, DatasourceSchemaDTO> dsMap = new HashMap<>();
        DatasourceSchemaDTO ds = new DatasourceSchemaDTO();
        ds.setType("mysql");
        dsMap.put(1L, ds);

        Dimension2SQLObj.dimension2sqlObj(
                meta, fields, originFields,
                false, dsMap, new ArrayList<>(), new ArrayList<>(), null);

        List<SQLObj> xOrders = meta.getXOrders();
        assertNotNull(xOrders);
        assertEquals(1, xOrders.size());
        String orderAlias = xOrders.get(0).getOrderAlias();
        assertNotNull(orderAlias);
        assertTrue(orderAlias.contains("CASE"), "custom_sort 应生成 CASE WHEN: " + orderAlias);
        assertTrue(orderAlias.contains("WHEN '开发一室' THEN 0"), "CASE 应含自定义顺序: " + orderAlias);
        assertTrue(orderAlias.contains("WHEN '数据应用室' THEN 4"), "CASE 应含最后一个值: " + orderAlias);
        assertTrue(orderAlias.contains("ELSE 5"), "CASE 应把未定义值放最后: " + orderAlias);
        assertEquals("ASC", xOrders.get(0).getOrderDirection());
    }

    @Test
    void customSortShouldEscapeSingleQuoteToAvoidInjection() {
        SQLMeta meta = new SQLMeta();
        meta.setTable(SQLObj.builder().tableName("t").tableAlias("t_a_0").build());

        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(2L);
        field.setOriginName("vendor");
        field.setEngineFieldName("f_vendor");
        field.setName("供应商");
        field.setGroupType("d");
        field.setFieldType(FieldTypeConstants.STRING);
        field.setExtField(ExtFieldConstant.EXT_NORMAL);
        field.setSort("custom_sort");
        // 含单引号的值，验证转义
        field.setCustomSort(List.of("O'Brien", "Normal"));

        List<ChartViewFieldDTO> fields = new ArrayList<>(List.of(field));
        List<DatasetTableFieldDTO> originFields = new ArrayList<>();
        originFields.add(copyBase(field));

        Map<Long, DatasourceSchemaDTO> dsMap = new HashMap<>();
        DatasourceSchemaDTO ds = new DatasourceSchemaDTO();
        ds.setType("mysql");
        dsMap.put(2L, ds);

        Dimension2SQLObj.dimension2sqlObj(
                meta, fields, originFields,
                false, dsMap, new ArrayList<>(), new ArrayList<>(), null);

        String orderAlias = meta.getXOrders().get(0).getOrderAlias();
        assertTrue(orderAlias.contains("WHEN 'O''Brien' THEN 0"), "单引号应转义为 '': " + orderAlias);
    }

    @Test
    void esDatasourceShouldSkipCustomSortCaseWhen() {
        SQLMeta meta = new SQLMeta();
        meta.setTable(SQLObj.builder().tableName("t").tableAlias("t_a_0").build());

        ChartViewFieldDTO field = new ChartViewFieldDTO();
        field.setId(3L);
        field.setOriginName("dept");
        field.setEngineFieldName("f_dept");
        field.setName("处室");
        field.setGroupType("d");
        field.setFieldType(FieldTypeConstants.STRING);
        field.setExtField(ExtFieldConstant.EXT_NORMAL);
        field.setSort("custom_sort");
        field.setCustomSort(List.of("A", "B"));

        List<ChartViewFieldDTO> fields = new ArrayList<>(List.of(field));
        List<DatasetTableFieldDTO> originFields = new ArrayList<>();
        originFields.add(copyBase(field));

        Map<Long, DatasourceSchemaDTO> dsMap = new HashMap<>();
        DatasourceSchemaDTO ds = new DatasourceSchemaDTO();
        ds.setType("es"); // elasticsearch 不下推 CASE
        dsMap.put(3L, ds);

        Dimension2SQLObj.dimension2sqlObj(
                meta, fields, originFields,
                false, dsMap, new ArrayList<>(), new ArrayList<>(), null);

        // ES 数据源不应生成 CASE WHEN order
        List<SQLObj> xOrders = meta.getXOrders();
        assertTrue(xOrders == null || xOrders.isEmpty(), "ES 数据源应跳过 custom_sort CASE WHEN");
    }

    private static DatasetTableFieldDTO copyBase(ChartViewFieldDTO src) {
        DatasetTableFieldDTO dto = new DatasetTableFieldDTO();
        dto.setId(src.getId());
        dto.setOriginName(src.getOriginName());
        dto.setEngineFieldName(src.getEngineFieldName());
        dto.setName(src.getName());
        dto.setFieldType(src.getFieldType());
        return dto;
    }
}
