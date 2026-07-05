package io.crest.datasource.provider;


import io.crest.dataset.utils.TableUtils;
import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.security.SqlSecurityPolicy;
import io.crest.datasource.server.DatasourceServer;
import io.crest.extensions.datasource.dto.TableField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("h2Engine")
public class H2EngineProvider extends EngineProvider {

    private static final String creatTableSql =
            "CREATE TABLE IF NOT EXISTS \"TABLE_NAME\"" +
                    "Column_Fields;";


    @Override
    public String createView(String name, String viewSQL) {
        return "CREATE or replace view " + h2Identifier(name) + " AS (" + SqlSecurityPolicy.validateReadQuery(viewSQL) + ")";
    }

    @Override
    public String insertSql(String dsType, String tableName, DatasourceServer.UpdateType extractType, List<String[]> dataList, int page, int pageNumber, List<TableField> tableFields) {
        String engineTableName;
        switch (extractType) {
            case all_scope:
                engineTableName = TableUtils.tmpName(TableUtils.tableName(tableName));
                break;
            default:
                engineTableName = TableUtils.tableName(tableName);
                break;
        }
        return "INSERT INTO  TABLE_NAME VALUES ".replace("TABLE_NAME", h2Identifier(engineTableName))
                + insertPlaceholders(dataList, page, pageNumber, tableFields);
    }


    @Override
    public String dropTable(String name) {
        return "DROP TABLE IF EXISTS " + h2Identifier(name);
    }

    @Override
    public String dropView(String name) {
        return "DROP VIEW IF EXISTS " + h2Identifier(name);
    }

    @Override
    public String replaceTable(String name) {
        String tmpName = TableUtils.tmpName(name);
        String backupName = name + "_tmp";
        return "ALTER TABLE " + h2Identifier(name) + " rename to " + h2Identifier(backupName)
                + "; ALTER TABLE " + h2Identifier(tmpName) + " rename to " + h2Identifier(name)
                + "; DROP TABLE IF EXISTS " + h2Identifier(backupName) + ";";
    }


    @Override
    public String createTableSql(String tableName, List<TableField> tableFields, CoreEngine engine) {
        validateSqlInjectionRisk(tableName);
        String dorisTableColumnSql = createTableSql(tableFields);
        return creatTableSql.replace("TABLE_NAME", escapedH2Identifier(tableName)).replace("Column_Fields", dorisTableColumnSql);
    }

    private String createTableSql(final List<TableField> tableFields) {
        StringBuilder columnFields = new StringBuilder("\"");
        StringBuilder key = new StringBuilder();
        for (TableField tableField : tableFields) {
            if (!tableField.isChecked()) {
                continue;
            }
            String fieldName = escapedH2Identifier(physicalFieldName(tableField));
            if (tableField.isPrimaryKey()) {
                key.append("\"").append(fieldName).append("\", ");
            }
            columnFields.append(fieldName).append("\" ");
            int size = tableField.getPrecision() * 4;
            switch (tableField.getExtractedFieldType()) {
                case 0:
                    String columnLength = varcharColumnLength(tableField.getLength());
                    if (StringUtils.isNotEmpty(columnLength)) {
                        columnFields.append("varchar(length)".replace("length", columnLength)).append(",\"");
                    } else {
                        columnFields.append("longtext").append(",\"");
                    }
                    break;
                case 1:
                    columnFields.append("varchar(2048)").append(",\"");
                    break;
                case 2:
                    columnFields.append("bigint(20)").append(",\"");
                    break;
                case 3:
                    columnFields.append("decimal(27,8)").append(",\"");
                    break;
                case 4:
                    columnFields.append("TINYINT(length)".replace("length", String.valueOf(tableField.getPrecision()))).append(",\"");
                    break;
                default:
                    columnFields.append("longtext").append(",\"");
                    break;
            }
        }
        if (StringUtils.isEmpty(key.toString())) {
            columnFields = new StringBuilder(columnFields.substring(0, columnFields.length() - 2));
        } else {
            key = new StringBuilder(key.substring(0, key.length() - 2));
            columnFields = new StringBuilder(columnFields.substring(0, columnFields.length() - 1));
            columnFields.append("PRIMARY KEY (PRIMARYKEY)".replace("PRIMARYKEY", key.toString()));
        }
        columnFields = new StringBuilder("(" + columnFields + ")");
        return columnFields.toString();
    }
}
