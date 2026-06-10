package io.crest.dataset.sync;

import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.commons.constants.TaskStatus;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTask;
import io.crest.engine.constant.ExtFieldConstant;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DatasetSyncUtils {

    public static final String CACHE_TABLE_PREFIX = "dataset_sync_";
    public static final int DEFAULT_FULL_SYNC_INTERVAL_HOURS = 24;
    public static final int DEFAULT_CACHE_EXPIRE_HOURS = 26;
    public static final int DEFAULT_TASK_TIMEOUT_MINUTES = 360;
    public static final int DEFAULT_FAILURE_WARN_THRESHOLD = 1;
    private static final Set<String> SUPPORTED_CACHE_SOURCE_TYPES = Set.of(
            DatasourceConfiguration.DatasourceType.obOracle.name(),
            DatasourceConfiguration.DatasourceType.mysql.name()
    );

    private DatasetSyncUtils() {
    }

    public static String cacheTableName(Long datasetGroupId) {
        return CACHE_TABLE_PREFIX + datasetGroupId;
    }

    public static String tmpCacheTableName(Long datasetGroupId) {
        return "tmp_" + cacheTableName(datasetGroupId);
    }

    public static boolean shouldRouteToCache(DatasetGroupInfoDTO dataset, Map<String, Object> sqlMap) {
        if (dataset == null || sqlMap == null || !Objects.equals(dataset.getMode(), 1)
                || Objects.equals(dataset.getIsCross(), true)) {
            return false;
        }
        Object dsMapObject = sqlMap.get("dsMap");
        if (!(dsMapObject instanceof Map<?, ?> dsMap) || dsMap.size() != 1) {
            return false;
        }
        if (hasSqlParameters(dataset, sqlMap)) {
            return false;
        }
        Object datasourceObject = dsMap.values().iterator().next();
        if (!(datasourceObject instanceof DatasourceSchemaDTO datasource)) {
            return false;
        }
        return isSupportedCacheSource(datasource.getType());
    }

    public static boolean hasSqlParameters(DatasetGroupInfoDTO dataset, Map<String, Object> sqlMap) {
        return hasSqlParameterValues(sqlMap) || hasSqlParameterDefinitions(dataset);
    }

    public static boolean hasSqlParameterValues(Map<String, Object> sqlMap) {
        if (sqlMap == null) {
            return false;
        }
        Object tableFieldWithValues = sqlMap.get("tableFieldWithValues");
        if (tableFieldWithValues instanceof Collection<?> values) {
            return !values.isEmpty();
        }
        return tableFieldWithValues != null;
    }

    private static boolean hasSqlParameterDefinitions(DatasetGroupInfoDTO dataset) {
        if (dataset == null || dataset.getUnion() == null) {
            return false;
        }
        return dataset.getUnion().stream().anyMatch(DatasetSyncUtils::hasSqlParameterDefinitions);
    }

    private static boolean hasSqlParameterDefinitions(UnionDTO union) {
        if (union == null) {
            return false;
        }
        DatasetTableDTO currentDs = union.getCurrentDs();
        if (currentDs != null && hasSqlVariableDefinitions(currentDs.getSqlVariableDetails())) {
            return true;
        }
        List<UnionDTO> children = union.getChildrenDs();
        return children != null && children.stream().anyMatch(DatasetSyncUtils::hasSqlParameterDefinitions);
    }

    private static boolean hasSqlVariableDefinitions(String sqlVariableDetails) {
        String value = StringUtils.trimToEmpty(sqlVariableDetails);
        return StringUtils.isNotBlank(value) && !Strings.CS.equals(value, "[]");
    }

    public static boolean isSupportedCacheSource(String datasourceType) {
        return SUPPORTED_CACHE_SOURCE_TYPES.stream().anyMatch(type -> Strings.CI.equals(type, datasourceType));
    }

    public static boolean isCacheReady(CoreDatasetSyncTask task) {
        return task != null && Objects.equals(task.getCacheReady(), 1);
    }

    public static boolean isCacheReady(CoreDatasetSyncTask task, String schemaHash) {
        return isCacheReady(task) && Strings.CS.equals(task.getSchemaHash(), schemaHash);
    }

    public static boolean canRunIncremental(CoreDatasetSyncTask task, String schemaHash) {
        return task != null
                && Strings.CI.equals(task.getUpdateType(), "add_scope")
                && StringUtils.isNotBlank(task.getIncrementalLastValue())
                && isCacheReady(task, schemaHash);
    }

    public static boolean shouldRunFullCalibration(CoreDatasetSyncTask task, long now) {
        if (task == null || !Strings.CI.equals(task.getUpdateType(), "add_scope")) {
            return false;
        }
        int intervalHours = Objects.requireNonNullElse(task.getFullSyncIntervalHours(), DEFAULT_FULL_SYNC_INTERVAL_HOURS);
        if (intervalHours <= 0) {
            return false;
        }
        Long lastFullSyncTime = task.getLastFullSyncTime();
        if (lastFullSyncTime == null || lastFullSyncTime <= 0) {
            return true;
        }
        return now - lastFullSyncTime >= intervalHours * 60L * 60L * 1000L;
    }

    public static boolean isCacheExpired(CoreDatasetSyncTask task, long now) {
        if (!isCacheReady(task) || task.getLastExecTime() == null || task.getLastExecTime() <= 0) {
            return false;
        }
        int expireHours = Objects.requireNonNullElse(task.getCacheExpireHours(), DEFAULT_CACHE_EXPIRE_HOURS);
        if (expireHours <= 0) {
            return false;
        }
        return now - task.getLastExecTime() > expireHours * 60L * 60L * 1000L;
    }

    public static boolean isTaskTimedOut(CoreDatasetSyncTask task, long startTime, long now) {
        int timeoutMinutes = Objects.requireNonNullElse(task == null ? null : task.getTaskTimeoutMinutes(), DEFAULT_TASK_TIMEOUT_MINUTES);
        return timeoutMinutes > 0 && now - startTime > timeoutMinutes * 60L * 1000L;
    }

    public static boolean isFailureWarned(CoreDatasetSyncTask task) {
        if (task == null) {
            return false;
        }
        int failures = Objects.requireNonNullElse(task.getConsecutiveFailures(), 0);
        int threshold = Objects.requireNonNullElse(task.getFailureWarnThreshold(), DEFAULT_FAILURE_WARN_THRESHOLD);
        return threshold > 0 && failures >= threshold;
    }

    public static boolean isTaskRunnable(CoreDatasetSyncTask task) {
        return task != null
                && !Strings.CI.equalsAny(task.getTaskStatus(), TaskStatus.Stopped.name(), TaskStatus.Suspend.name());
    }

    public static ReconcileResult reconcile(Long sourceRowCount, Long cacheRowCount, String sourceWatermark, String cacheWatermark) {
        if (!Objects.equals(sourceRowCount, cacheRowCount)) {
            return new ReconcileResult("WARNING", "源端行数 " + sourceRowCount + " 与缓存行数 " + cacheRowCount + " 不一致");
        }
        if (StringUtils.isNotBlank(sourceWatermark) || StringUtils.isNotBlank(cacheWatermark)) {
            if (!Strings.CS.equals(StringUtils.defaultString(sourceWatermark), StringUtils.defaultString(cacheWatermark))) {
                return new ReconcileResult(
                        "WARNING",
                        "源端最大水位 " + sourceWatermark + " 与缓存最大水位 " + cacheWatermark + " 不一致"
                );
            }
        }
        return new ReconcileResult("PASSED", "对账通过");
    }

    public static List<TableField> toEngineTableFields(List<DatasetTableFieldDTO> fields) {
        List<TableField> tableFields = new ArrayList<>();
        if (fields == null) {
            return tableFields;
        }
        for (DatasetTableFieldDTO field : fields) {
            if (!Objects.equals(field.getChecked(), true)) {
                continue;
            }
            if (!Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL)) {
                continue;
            }
            String columnName = StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName());
            if (StringUtils.isBlank(columnName)) {
                continue;
            }
            TableField tableField = new TableField();
            tableField.setName(columnName);
            tableField.setOriginName(columnName);
            tableField.setNativeType(field.getName());
            tableField.setType(field.getType());
            tableField.setExtractedFieldType(field.getExtractedFieldType());
            tableField.setFieldType(field.getFieldType());
            tableField.setChecked(true);
            tableFields.add(tableField);
        }
        return tableFields;
    }

    public static String buildIncrementalPredicate(DatasetTableFieldDTO field, String lastValue, String prefix, String suffix) {
        return buildIncrementalPredicate(field, lastValue, prefix, suffix, false);
    }

    public static String buildIncrementalPredicate(DatasetTableFieldDTO field, String lastValue, String prefix, String suffix, boolean inclusive) {
        return buildIncrementalPredicate(field, lastValue, prefix, suffix, inclusive, DatasourceConfiguration.DatasourceType.obOracle.name());
    }

    public static String buildIncrementalPredicate(DatasetTableFieldDTO field, String lastValue, String prefix, String suffix, boolean inclusive, String datasourceType) {
        String column = prefix + StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()) + suffix;
        String operator = inclusive ? " >= " : " > ";
        if (Objects.equals(field.getExtractedFieldType(), 1) || Objects.equals(field.getFieldType(), 1)) {
            if (Strings.CI.equals(datasourceType, DatasourceConfiguration.DatasourceType.mysql.name())) {
                return column + operator + "'" + escapeSqlLiteral(lastValue) + "'";
            }
            return column + operator + "TO_TIMESTAMP('" + escapeSqlLiteral(lastValue) + "', 'YYYY-MM-DD HH24:MI:SS.FF')";
        }
        if (Objects.equals(field.getExtractedFieldType(), 2) || Objects.equals(field.getExtractedFieldType(), 3)
                || Objects.equals(field.getFieldType(), 2) || Objects.equals(field.getFieldType(), 3)) {
            return column + operator + lastValue;
        }
        return column + operator + "'" + escapeSqlLiteral(lastValue) + "'";
    }

    public static String buildCacheWatermarkPredicate(DatasetTableFieldDTO field, String lastValue, String prefix, String suffix, String operator) {
        String column = prefix + StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()) + suffix;
        String safeOperator = StringUtils.defaultIfBlank(operator, ">").trim();
        if (isNumericField(field) && isNumericLiteral(lastValue)) {
            return column + " " + safeOperator + " " + lastValue;
        }
        return column + " " + safeOperator + " '" + escapeSqlLiteral(lastValue) + "'";
    }

    public static boolean isWatermarkCompatible(DatasetTableFieldDTO field, String lastValue) {
        if (field == null || StringUtils.isBlank(lastValue)) {
            return false;
        }
        return !isNumericField(field) || isNumericLiteral(lastValue);
    }

    private static boolean isNumericField(DatasetTableFieldDTO field) {
        return Objects.equals(field.getExtractedFieldType(), 2) || Objects.equals(field.getExtractedFieldType(), 3)
                || Objects.equals(field.getFieldType(), 2) || Objects.equals(field.getFieldType(), 3);
    }

    private static boolean isNumericLiteral(String value) {
        return StringUtils.defaultString(value).trim().matches("-?\\d+(\\.\\d+)?");
    }

    public static String buildCacheSelectSql(Long datasetGroupId, List<DatasetTableFieldDTO> fields, String prefix, String suffix, String schemaAlias) {
        String selectFields = fields.stream()
                .filter(field -> Objects.equals(field.getChecked(), true))
                .filter(field -> Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL))
                .map(field -> StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()))
                .filter(StringUtils::isNotBlank)
                .map(fieldName -> quote(fieldName, prefix, suffix) + " AS " + quote(fieldName, prefix, suffix))
                .collect(Collectors.joining(","));
        String table = quote(cacheTableName(datasetGroupId), prefix, suffix);
        if (StringUtils.isNotBlank(schemaAlias)) {
            table = quote(schemaAlias, prefix, suffix) + "." + table;
        }
        return "SELECT " + selectFields + " FROM " + table;
    }

    public static String buildOraclePageSql(String sourceSql, int limit, int offset, List<DatasetTableFieldDTO> fields, String prefix, String suffix) {
        String selectFields = fields.stream()
                .filter(field -> Objects.equals(field.getChecked(), true))
                .filter(field -> Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL))
                .map(field -> StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()))
                .filter(StringUtils::isNotBlank)
                .map(fieldName -> "SYNC_PAGE." + quote(fieldName, prefix, suffix))
                .collect(Collectors.joining(","));
        return "SELECT " + selectFields
                + " FROM (SELECT SYNC_SRC.*, ROWNUM AS SYNC_ROWNUM FROM (" + sourceSql + ") SYNC_SRC WHERE ROWNUM <= "
                + (offset + limit) + ") SYNC_PAGE WHERE SYNC_ROWNUM > " + offset;
    }

    public static String buildSourcePageSql(String datasourceType, String sourceSql, int limit, int offset,
                                            List<DatasetTableFieldDTO> fields, String prefix, String suffix) {
        if (Strings.CI.equals(datasourceType, DatasourceConfiguration.DatasourceType.mysql.name())) {
            return buildMysqlPageSql(sourceSql, limit, offset, fields, prefix, suffix);
        }
        if (Strings.CI.equals(datasourceType, DatasourceConfiguration.DatasourceType.obOracle.name())) {
            return buildOraclePageSql(sourceSql, limit, offset, fields, prefix, suffix);
        }
        throw new IllegalArgumentException("Unsupported dataset cache source type: " + datasourceType);
    }

    public static String buildMysqlPageSql(String sourceSql, int limit, int offset, List<DatasetTableFieldDTO> fields, String prefix, String suffix) {
        String selectFields = fields.stream()
                .filter(field -> Objects.equals(field.getChecked(), true))
                .filter(field -> Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL))
                .map(field -> StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()))
                .filter(StringUtils::isNotBlank)
                .map(fieldName -> "SYNC_PAGE." + quote(fieldName, prefix, suffix))
                .collect(Collectors.joining(","));
        String orderFields = fields.stream()
                .filter(field -> Objects.equals(field.getChecked(), true))
                .filter(field -> Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL))
                .map(field -> StringUtils.defaultIfBlank(field.getEngineFieldName(), field.getFieldShortName()))
                .filter(StringUtils::isNotBlank)
                .map(fieldName -> "SYNC_PAGE." + quote(fieldName, prefix, suffix))
                .collect(Collectors.joining(","));
        String orderClause = StringUtils.isBlank(orderFields) ? "" : " ORDER BY " + orderFields;
        return "SELECT " + selectFields + " FROM (" + sourceSql + ") SYNC_PAGE" + orderClause
                + " LIMIT " + limit + " OFFSET " + offset;
    }

    public static String quote(String value, String prefix, String suffix) {
        return prefix + value + suffix;
    }

    public static String schemaHash(List<DatasetTableFieldDTO> fields) {
        String signature = fields.stream()
                .filter(field -> Objects.equals(field.getChecked(), true))
                .filter(field -> Objects.equals(field.getExtField(), ExtFieldConstant.EXT_NORMAL))
                .map(field -> String.join("|",
                        StringUtils.defaultString(field.getEngineFieldName()),
                        StringUtils.defaultString(field.getFieldShortName()),
                        StringUtils.defaultString(field.getName()),
                        StringUtils.defaultString(field.getOriginName()),
                        String.valueOf(field.getExtractedFieldType()),
                        String.valueOf(field.getFieldType())
                ))
                .collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(signature.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String escapeSqlLiteral(String value) {
        return StringUtils.defaultString(value).replace("'", "''");
    }

    public record ReconcileResult(String status, String message) {
    }
}
