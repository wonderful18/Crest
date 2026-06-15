package io.crest.engine.trans;

import io.crest.api.chart.dto.ChartSortFieldDTO;
import io.crest.constant.SQLConstants;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

// 定义当前业务对象的核心职责
public class DatasetOrder2SQLObj {

    // 执行当前服务方法并返回处理结果
    public static void getOrders(SQLMeta meta, List<ChartSortFieldDTO> sortFields, List<DatasetTableFieldDTO> originFields) {
        SQLObj tableObj = meta.getTable();
        List<SQLObj> xOrders = meta.getXOrders() == null ? new ArrayList<>() : meta.getXOrders();
        if (ObjectUtils.isEmpty(tableObj)) {
            return;
        }
        if (ObjectUtils.isNotEmpty(sortFields)) {
            for (int i = 0; i < sortFields.size(); i++) {
                ChartSortFieldDTO sortField = sortFields.get(i);
                for (int j = 0; j < originFields.size(); j++) {
                    if (sortField.getId().equals(originFields.get(j).getId())) {
                        String fieldAlias = String.format(SQLConstants.FIELD_ALIAS_X_PREFIX, j);
                        SQLObj order = SQLObj.builder()
                                .orderField(String.format(SQLConstants.FIELD_DOT, fieldAlias))
                                .orderAlias(String.format(SQLConstants.FIELD_DOT, fieldAlias))
                                .orderDirection(sortField.getOrderDirection()).build();
                        xOrders.add(order);
                    }
                }


            }
            meta.setXOrders(xOrders);
        }
    }

}
