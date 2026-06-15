package io.crest.dataset.sync;

import io.crest.engine.constant.ExtFieldConstant;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.commons.constants.TaskStatus;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTask;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.dto.TableField;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 覆盖数据集同步工具的缓存命名、路由判断和同步 SQL 生成规则。
public class DatasetSyncUtilsTest {

    // 验证缓存表名使用数据集 ID 作为命名空间。
    @Test
    public void cacheTableNameUsesDatasetIdNamespace() {
        assertEquals("dataset_sync_12345", DatasetSyncUtils.cacheTableName(12345L));
        assertEquals("tmp_dataset_sync_12345", DatasetSyncUtils.tmpCacheTableName(12345L));
    }

    // 验证引擎字段生成时跳过计算字段并保留引擎字段名。
    @Test
    public void toEngineTableFieldsUsesEngineFieldNamesAndSkipsCalculatedFields() {
        DatasetTableFieldDTO normal = field(1L, "USER_ID", "用户ID", "f_user_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO calc = field(2L, "sum([1])", "计算字段", "f_calc", 3, ExtFieldConstant.EXT_CALC);

        List<TableField> fields = DatasetSyncUtils.toEngineTableFields(List.of(normal, calc));

        assertEquals(1, fields.size());
        assertEquals("f_user_id", fields.get(0).getName());
        assertEquals("f_user_id", fields.get(0).getOriginName());
        assertEquals("用户ID", fields.get(0).getNativeType());
        assertEquals(Integer.valueOf(2), fields.get(0).getFieldType());
        assertEquals(Integer.valueOf(2), fields.get(0).getExtractedFieldType());
    }

    // 验证增量谓词按字段类型格式化比较值。
    @Test
    public void buildIncrementalPredicateFormatsValuesByFieldType() {
        DatasetTableFieldDTO numberField = field(1L, "ID", "ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO textField = field(2L, "CODE", "CODE", "f_code", 0, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO timeField = field(3L, "UPDATED_AT", "更新时间", "f_updated_at", 1, ExtFieldConstant.EXT_NORMAL);

        assertEquals("\"f_id\" > 100", DatasetSyncUtils.buildIncrementalPredicate(numberField, "100", "\"", "\""));
        assertEquals("\"f_code\" > 'A''01'", DatasetSyncUtils.buildIncrementalPredicate(textField, "A'01", "\"", "\""));
        assertEquals(
                "\"f_updated_at\" > TO_TIMESTAMP('2026-05-14 12:30:01', 'YYYY-MM-DD HH24:MI:SS.FF')",
                DatasetSyncUtils.buildIncrementalPredicate(timeField, "2026-05-14 12:30:01", "\"", "\"")
        );
    }

    // 验证 MySQL 时间字段使用普通字符串时间字面量。
    @Test
    public void buildIncrementalPredicateUsesMysqlLiteralForMysqlTimeField() {
        DatasetTableFieldDTO timeField = field(3L, "UPDATED_AT", "更新时间", "f_updated_at", 1, ExtFieldConstant.EXT_NORMAL);

        assertEquals(
                "`f_updated_at` >= '2026-05-14 12:30:01.0'",
                DatasetSyncUtils.buildIncrementalPredicate(timeField, "2026-05-14 12:30:01.0", "`", "`", true, "mysql")
        );
    }

    // 验证增量回放可包含上一批水位值。
    @Test
    public void buildIncrementalPredicateCanReplayLastWatermarkBucket() {
        DatasetTableFieldDTO timeField = field(3L, "UPDATED_AT", "更新时间", "f_updated_at", 1, ExtFieldConstant.EXT_NORMAL);

        assertEquals(
                "\"f_updated_at\" >= TO_TIMESTAMP('2026-05-14 12:30:01.0', 'YYYY-MM-DD HH24:MI:SS.FF')",
                DatasetSyncUtils.buildIncrementalPredicate(timeField, "2026-05-14 12:30:01.0", "\"", "\"", true)
        );
        assertEquals(
                "`f_updated_at` >= '2026-05-14 12:30:01.0'",
                DatasetSyncUtils.buildCacheWatermarkPredicate(timeField, "2026-05-14 12:30:01.0", "`", "`", ">=")
        );
        assertEquals(
                "`f_updated_at` < '2026-05-14 12:30:01.0'",
                DatasetSyncUtils.buildCacheWatermarkPredicate(timeField, "2026-05-14 12:30:01.0", "`", "`", "<")
        );
    }

    // 验证数值字段遇到旧格式非数值水位时按字符串兜底。
    @Test
    public void cacheWatermarkPredicateQuotesStaleNonNumericWatermarkForNumericField() {
        DatasetTableFieldDTO numberField = field(1L, "ID", "主键ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);

        assertFalse(DatasetSyncUtils.isWatermarkCompatible(numberField, "2026-05-15 09:35:00.0"));
        assertTrue(DatasetSyncUtils.isWatermarkCompatible(numberField, "100"));
        assertEquals(
                "\"f_id\" < '2026-05-15 09:35:00.0'",
                DatasetSyncUtils.buildCacheWatermarkPredicate(numberField, "2026-05-15 09:35:00.0", "\"", "\"", "<")
        );
    }

    // 验证字段结构变化会影响 Schema 哈希。
    @Test
    public void schemaHashChangesWhenSelectedFieldShapeChanges() {
        DatasetTableFieldDTO id = field(1L, "ID", "ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO name = field(2L, "NAME", "名称", "f_name", 0, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO renamed = field(2L, "NAME", "名称", "f_user_name", 0, ExtFieldConstant.EXT_NORMAL);

        String schemaHash = DatasetSyncUtils.schemaHash(List.of(id, name));

        assertEquals(schemaHash, DatasetSyncUtils.schemaHash(List.of(id, name)));
        assertFalse(schemaHash.equals(DatasetSyncUtils.schemaHash(List.of(id, renamed))));
    }

    // 验证缓存查询 SQL 读取缓存表中的引擎字段。
    @Test
    public void buildCacheSelectSqlReadsEngineColumnsFromCacheTable() {
        DatasetTableFieldDTO id = field(1L, "ID", "ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO name = field(2L, "NAME", "名称", "f_name", 0, ExtFieldConstant.EXT_NORMAL);

        String sql = DatasetSyncUtils.buildCacheSelectSql(8L, List.of(id, name), "`", "`", "s_a_1");

        assertEquals(
                "SELECT `f_id` AS `f_id`,`f_name` AS `f_name` FROM `s_a_1`.`dataset_sync_8`",
                sql
        );
    }

    // 验证 Oracle 分页 SQL 仅保留可同步列并隐藏行号列。
    @Test
    public void buildOraclePageSqlKeepsOnlySyncableColumnsAndHidesRowNumber() {
        DatasetTableFieldDTO id = field(1L, "ID", "ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO calc = field(2L, "[1] + 1", "计算字段", "f_calc", 2, ExtFieldConstant.EXT_CALC);

        String sql = DatasetSyncUtils.buildOraclePageSql(
                "SELECT t_a_0.\"ID\" AS \"f_id\" FROM \"T_USER\" t_a_0",
                1000,
                2000,
                List.of(id, calc),
                "\"",
                "\""
        );

        assertEquals(
                "SELECT SYNC_PAGE.\"f_id\" FROM (SELECT SYNC_SRC.*, ROWNUM AS SYNC_ROWNUM FROM (SELECT t_a_0.\"ID\" AS \"f_id\" FROM \"T_USER\" t_a_0) SYNC_SRC WHERE ROWNUM <= 3000) SYNC_PAGE WHERE SYNC_ROWNUM > 2000",
                sql
        );
    }

    // 验证 MySQL 分页 SQL 使用稳定排序并仅保留可同步列。
    @Test
    public void buildMysqlPageSqlUsesStableOrderAndKeepsOnlySyncableColumns() {
        DatasetTableFieldDTO id = field(1L, "ID", "ID", "f_id", 2, ExtFieldConstant.EXT_NORMAL);
        DatasetTableFieldDTO calc = field(2L, "[1] + 1", "计算字段", "f_calc", 2, ExtFieldConstant.EXT_CALC);

        String sql = DatasetSyncUtils.buildSourcePageSql(
                "mysql",
                "SELECT t_a_0.`ID` AS `f_id` FROM `t_user` t_a_0",
                1000,
                2000,
                List.of(id, calc),
                "`",
                "`"
        );

        assertEquals(
                "SELECT SYNC_PAGE.`f_id` FROM (SELECT t_a_0.`ID` AS `f_id` FROM `t_user` t_a_0) SYNC_PAGE ORDER BY SYNC_PAGE.`f_id` LIMIT 1000 OFFSET 2000",
                sql
        );
    }

    // 验证只有单数据源且支持抽取的数据集才路由到缓存。
    @Test
    public void shouldRouteToCacheOnlyForSingleSupportedExtractDataset() {
        assertTrue(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap("obOracle")));
        assertTrue(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap("mysql")));

        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(0, false), sqlMap("obOracle")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, true), sqlMap("obOracle")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap("Excel")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap("API")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap("obOracle", "mysql")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(null, sqlMap("obOracle")));
        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), null));
    }

    // 验证带参数值的 SQL 数据集不会路由到缓存。
    @Test
    public void shouldNotRouteParameterizedSqlDatasetToCache() {
        Map<String, Object> sqlMap = sqlMap("mysql");
        sqlMap.put("tableFieldWithValues", List.of(new Object()));

        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset(1, false), sqlMap));
    }

    // 验证未准备变量值的 SQL 变量数据集不会路由到缓存。
    @Test
    public void shouldNotRouteSqlVariableDatasetToCacheWithoutPreparedValues() {
        DatasetGroupInfoDTO dataset = dataset(1, false);
        dataset.setUnion(List.of(unionWithSqlVariables("[{\"variableName\":\"dept\"}]")));

        assertFalse(DatasetSyncUtils.shouldRouteToCache(dataset, sqlMap("mysql")));
    }

    // 验证同步校验能识别 SQL 参数值。
    @Test
    public void detectsParameterizedSqlValuesForSyncValidation() {
        assertFalse(DatasetSyncUtils.hasSqlParameterValues(null));

        Map<String, Object> sqlMap = sqlMap("mysql");
        assertFalse(DatasetSyncUtils.hasSqlParameterValues(sqlMap));

        sqlMap.put("tableFieldWithValues", List.of(new Object()));
        assertTrue(DatasetSyncUtils.hasSqlParameterValues(sqlMap));
    }

    // 验证缓存就绪状态依赖成功物化表和匹配的 Schema。
    @Test
    public void cacheReadyRequiresSuccessfulMaterializedTable() {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        assertFalse(DatasetSyncUtils.isCacheReady(null));
        assertFalse(DatasetSyncUtils.isCacheReady(task));

        task.setCacheReady(0);
        assertFalse(DatasetSyncUtils.isCacheReady(task));

        task.setCacheReady(1);
        assertTrue(DatasetSyncUtils.isCacheReady(task));

        task.setSchemaHash("schema-v1");
        assertTrue(DatasetSyncUtils.isCacheReady(task, "schema-v1"));
        assertFalse(DatasetSyncUtils.isCacheReady(task, "schema-v2"));
    }

    // 验证只有存在且运行中的任务才允许执行。
    @Test
    public void taskRunnableRequiresExistingActiveTask() {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();

        assertFalse(DatasetSyncUtils.isTaskRunnable(null));

        task.setTaskStatus(TaskStatus.UnderExecution.name());
        assertTrue(DatasetSyncUtils.isTaskRunnable(task));

        task.setTaskStatus(TaskStatus.Stopped.name());
        assertFalse(DatasetSyncUtils.isTaskRunnable(task));

        task.setTaskStatus(TaskStatus.Suspend.name());
        assertFalse(DatasetSyncUtils.isTaskRunnable(task));
    }

    // 验证增量执行依赖水位、缓存就绪和匹配的 Schema。
    @Test
    public void incrementalRequiresWatermarkReadyCacheAndMatchingSchema() {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        task.setUpdateType("add_scope");
        task.setIncrementalLastValue("2026-05-14 10:50:00.0");
        task.setCacheReady(1);
        task.setSchemaHash("schema-v1");

        assertTrue(DatasetSyncUtils.canRunIncremental(task, "schema-v1"));

        task.setSchemaHash("schema-v0");
        assertFalse(DatasetSyncUtils.canRunIncremental(task, "schema-v1"));

        task.setSchemaHash("schema-v1");
        task.setCacheReady(0);
        assertFalse(DatasetSyncUtils.canRunIncremental(task, "schema-v1"));

        task.setCacheReady(1);
        task.setIncrementalLastValue(null);
        assertFalse(DatasetSyncUtils.canRunIncremental(task, "schema-v1"));

        task.setIncrementalLastValue("2026-05-14 10:50:00.0");
        task.setUpdateType("all_scope");
        assertFalse(DatasetSyncUtils.canRunIncremental(task, "schema-v1"));
    }

    // 验证全量校准只在增量任务超过配置间隔后触发。
    @Test
    public void fullCalibrationRunsOnlyWhenIncrementalTaskExceedsConfiguredInterval() {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        task.setUpdateType("add_scope");
        task.setFullSyncIntervalHours(24);
        task.setLastFullSyncTime(1_000L);

        assertFalse(DatasetSyncUtils.shouldRunFullCalibration(task, 1_000L + 23 * 60 * 60 * 1000L));
        assertTrue(DatasetSyncUtils.shouldRunFullCalibration(task, 1_000L + 24 * 60 * 60 * 1000L));

        task.setFullSyncIntervalHours(0);
        assertFalse(DatasetSyncUtils.shouldRunFullCalibration(task, 1_000L + 48 * 60 * 60 * 1000L));

        task.setFullSyncIntervalHours(24);
        task.setUpdateType("all_scope");
        assertFalse(DatasetSyncUtils.shouldRunFullCalibration(task, 1_000L + 48 * 60 * 60 * 1000L));
    }

    // 验证缓存超过配置阈值后被判定为过期。
    @Test
    public void cacheExpiresWhenLastSuccessfulSyncIsOlderThanConfiguredThreshold() {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        task.setCacheReady(1);
        task.setCacheExpireHours(26);
        task.setLastExecTime(1_000L);

        assertFalse(DatasetSyncUtils.isCacheExpired(task, 1_000L + 25 * 60 * 60 * 1000L));
        assertTrue(DatasetSyncUtils.isCacheExpired(task, 1_000L + 26 * 60 * 60 * 1000L + 1));

        task.setCacheReady(0);
        assertFalse(DatasetSyncUtils.isCacheExpired(task, 1_000L + 48 * 60 * 60 * 1000L));
    }

    // 验证对账结果能报告行数和水位不一致。
    @Test
    public void reconcileStatusReportsRowCountAndWatermarkMismatches() {
        DatasetSyncUtils.ReconcileResult passed = DatasetSyncUtils.reconcile(10L, 10L, "2026-05-15 10:00:00.0", "2026-05-15 10:00:00.0");
        assertEquals("PASSED", passed.status());
        assertEquals("源数据与缓存一致", passed.message());

        DatasetSyncUtils.ReconcileResult rowMismatch = DatasetSyncUtils.reconcile(10L, 12L, "2026-05-15 10:00:00.0", "2026-05-15 10:00:00.0");
        assertEquals("WARNING", rowMismatch.status());
        assertEquals("源数据行数 10 与缓存行数 12 不一致", rowMismatch.message());

        DatasetSyncUtils.ReconcileResult watermarkMismatch = DatasetSyncUtils.reconcile(10L, 10L, "2026-05-15 10:00:00.0", "2026-05-15 09:59:00.0");
        assertEquals("WARNING", watermarkMismatch.status());
        assertEquals("源数据最大增量值 2026-05-15 10:00:00.0 与缓存最大增量值 2026-05-15 09:59:00.0 不一致", watermarkMismatch.message());
    }

    // 构造测试用数据集字段。
    private DatasetTableFieldDTO field(Long id, String originName, String name, String engineFieldName, Integer fieldType, Integer extField) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(id);
        field.setOriginName(originName);
        field.setName(name);
        field.setEngineFieldName(engineFieldName);
        field.setFieldShortName(engineFieldName);
        field.setExtractedFieldType(fieldType);
        field.setFieldType(fieldType);
        field.setExtField(extField);
        field.setChecked(true);
        return field;
    }

    // 构造测试用数据集基础信息。
    private DatasetGroupInfoDTO dataset(Integer mode, Boolean isCross) {
        DatasetGroupInfoDTO dataset = new DatasetGroupInfoDTO();
        dataset.setMode(mode);
        dataset.setIsCross(isCross);
        return dataset;
    }

    // 构造带 SQL 变量详情的 union 节点。
    private UnionDTO unionWithSqlVariables(String sqlVariableDetails) {
        DatasetTableDTO table = new DatasetTableDTO();
        table.setSqlVariableDetails(sqlVariableDetails);
        UnionDTO union = new UnionDTO();
        union.setCurrentDs(table);
        return union;
    }

    // 构造数据源类型映射。
    private Map<String, Object> sqlMap(String... types) {
        Map<Long, DatasourceSchemaDTO> dsMap = new LinkedHashMap<>();
        long id = 1L;
        for (String type : types) {
            DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
            datasource.setId(id);
            datasource.setType(type);
            dsMap.put(id, datasource);
            id++;
        }
        Map<String, Object> sqlMap = new LinkedHashMap<>();
        sqlMap.put("dsMap", dsMap);
        return sqlMap;
    }
}
