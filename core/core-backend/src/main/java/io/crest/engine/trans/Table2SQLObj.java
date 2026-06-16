package io.crest.engine.trans;

import io.crest.constant.SQLConstants;
import io.crest.extensions.datasource.constant.SqlPlaceholderConstants;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.model.SQLObj;

// 定义当前业务对象的核心职责
public class Table2SQLObj {
    // 根据输入参数构造查询语句片段
    public static void table2sqlobj(SQLMeta meta, String tablePrefix, String table, boolean crossDs) {
        String sql;
        if (table.startsWith("(") && table.endsWith(")") && !crossDs) {// SQL片段和关联
            meta.setTableDialect(table.substring(1, table.length() - 1));
            sql = "(" + SqlPlaceholderConstants.TABLE_PLACEHOLDER + ")";
        } else {
            sql = table;
        }
        SQLObj tableObj = SQLObj.builder()
                .tableName((table.startsWith("(") && table.endsWith(")")) ? sql : String.format(SQLConstants.TABLE_NAME, tablePrefix, table))
                .tableAlias(String.format(SQLConstants.TABLE_ALIAS_PREFIX, 0))
                .build();
        meta.setTable(tableObj);
    }
}
