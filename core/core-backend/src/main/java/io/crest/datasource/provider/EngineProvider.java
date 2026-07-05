package io.crest.datasource.provider;

import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.request.EngineRequest;
import io.crest.datasource.server.DatasourceServer;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.extensions.datasource.dto.TableFieldWithValue;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static io.crest.engine.utils.Utils.SQL_INJECTION_PATTERNS;

public abstract class EngineProvider {
    private static final int MAX_VARCHAR_LENGTH = 65535;
    private static final Pattern COLUMN_LENGTH_PATTERN = Pattern.compile("\\d{1,5}");

    public abstract String createView(String name, String viewSQL);

    public abstract String dropTable(String name);

    public abstract String dropView(String name);

    public abstract String replaceTable(String name);

    public abstract String createTableSql(String name, List<TableField> tableFields, CoreEngine engine);

    public abstract String insertSql(String dsType, String tableName, DatasourceServer.UpdateType extractType, List<String[]> dataList, int page, int pageNumber, List<TableField> tableFields);

    public List<TableFieldWithValue> insertParams(String dsType, String tableName, DatasourceServer.UpdateType extractType, List<String[]> dataList, int page, int pageNumber, List<TableField> tableFields) {
        List<TableFieldWithValue> values = new ArrayList<>();
        for (String[] row : pageRows(dataList, page, pageNumber)) {
            for (int i = 0; i < tableFields.size(); i++) {
                if (!tableFields.get(i).isChecked()) {
                    continue;
                }
                String value = i < row.length ? row[i] : null;
                values.add(new TableFieldWithValue().setValue(StringUtils.isEmpty(value) ? null : value));
            }
        }
        return values;
    }

    protected String insertPlaceholders(List<String[]> dataList, int page, int pageNumber, List<TableField> tableFields) {
        int checkedFieldCount = (int) tableFields.stream().filter(TableField::isChecked).count();
        if (checkedFieldCount == 0) {
            CrestException.throwException("No checked fields");
        }
        List<String[]> rows = pageRows(dataList, page, pageNumber);
        if (rows.isEmpty()) {
            CrestException.throwException("No rows to insert");
        }
        String rowPlaceholder = "(" + String.join(",", Collections.nCopies(checkedFieldCount, "?")) + ")";
        return String.join(",", Collections.nCopies(rows.size(), rowPlaceholder));
    }

    protected List<String[]> pageRows(List<String[]> dataList, int page, int pageNumber) {
        int fromIndex = Math.max(0, (page - 1) * pageNumber);
        int toIndex = Math.min(page * pageNumber, dataList.size());
        if (fromIndex >= toIndex) {
            return List.of();
        }
        return dataList.subList(fromIndex, toIndex);
    }

    protected static String physicalFieldName(TableField field) {
        return StringUtils.defaultIfBlank(field.getDbFieldName(), field.getName());
    }

    protected static String varcharColumnLength(String value) {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (!COLUMN_LENGTH_PATTERN.matcher(normalized).matches()) {
            CrestException.throwException("Illegal column length");
        }
        int length = Integer.parseInt(normalized);
        if (length < 1 || length > MAX_VARCHAR_LENGTH) {
            CrestException.throwException("Illegal column length");
        }
        return String.valueOf(length);
    }

    protected static String mysqlIdentifier(String value) {
        return "`" + escapedMysqlIdentifier(value) + "`";
    }

    protected static String escapedMysqlIdentifier(String value) {
        validateSqlInjectionRisk(value);
        return StringUtils.defaultString(value).replace("`", "``");
    }

    protected static String h2Identifier(String value) {
        return "\"" + escapedH2Identifier(value) + "\"";
    }

    protected static String escapedH2Identifier(String value) {
        validateSqlInjectionRisk(value);
        return StringUtils.defaultString(value).replace("\"", "\"\"");
    }

    public static void validateSqlInjectionRisk(String value) {
        String normalized = StringUtils.defaultString(value);
        if (StringUtils.isEmpty(normalized)) {
            return;
        }
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                CrestException.throwException("Illegal table name");
            }
        }
    }
}
