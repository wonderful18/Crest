package io.crest.dataset.sync;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.crest.api.dataset.dto.DatasetSyncLogDTO;
import io.crest.api.dataset.dto.DatasetSyncTaskPageVO;
import io.crest.api.dataset.dto.DatasetSyncTaskRequest;
import io.crest.api.dataset.dto.DatasetSyncTaskDTO;
import io.crest.commons.constants.TaskStatus;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTask;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTaskLog;
import io.crest.dataset.dao.auto.mapper.CoreDatasetSyncTaskLogMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetSyncTaskMapper;
import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.manage.EngineManage;
import io.crest.datasource.provider.EngineProvider;
import io.crest.datasource.provider.ProviderUtil;
import io.crest.datasource.provider.CalciteProvider;
import io.crest.datasource.request.EngineRequest;
import io.crest.datasource.server.DatasourceTaskServer;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import io.crest.job.schedule.DatasetSyncJob;
import io.crest.job.schedule.ScheduleManager;
import io.crest.metadata.MetadataDbDialect;
import io.crest.metadata.MetadataDbDialects;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.BeanUtils;
import io.crest.utils.IDUtils;
import io.crest.utils.LogUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 数据集同步任务管理器，负责同步任务配置、任务中心列表、执行状态流转、调度注册和缓存表清理
 * 该类只维护任务元数据和调度边界，具体数据抽取、校验和缓存写入由同步执行流程承担
 */
@Component
public class DatasetSyncTaskManage {

    public static final String DEFAULT_UPDATE_TYPE = "all_scope";
    private static final long RUNNING_TASK_STALE_MILLIS = 12 * 60 * 60 * 1000L;
    private static final String JOB_GROUP_PREFIX = "dataset_sync_";

    @Resource
    private CoreDatasetSyncTaskMapper taskMapper;
    @Resource
    private CoreDatasetSyncTaskLogMapper logMapper;
    @Resource
    private ScheduleManager scheduleManager;
    @Resource
    private EngineManage engineManage;
    @Resource
    private CalciteProvider calciteProvider;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private PlatformPermissionManage platformPermissionManage;
    @Resource
    private Environment environment;

    /**
     * 按任务主键读取同步任务实体，供执行器和状态更新流程使用
     */
    public CoreDatasetSyncTask selectById(Long id) {
        return taskMapper.selectById(id);
    }

    /**
     * 按数据集 ID 读取唯一同步任务，数据集和同步任务保持一对一关系
     */
    public CoreDatasetSyncTask selectByDatasetGroupId(Long datasetGroupId) {
        QueryWrapper<CoreDatasetSyncTask> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);
        return taskMapper.selectOne(wrapper);
    }

    /**
     * 返回全部同步任务，通常用于系统启动后的调度恢复
     */
    public List<CoreDatasetSyncTask> listAll() {
        return taskMapper.selectList(null);
    }

    /**
     * 查询指定可视化依赖的同步数据集，结果带上最近一次同步日志和权限过滤条件
     */
    public List<DatasetSyncTaskDTO> dependencies(Long visualizationId) {
        if (visualizationId == null) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        args.add(visualizationId);
        return jdbcTemplate.query("""
                SELECT dg.id AS dataset_group_id,
                       dg.name AS dataset_name,
                       dg.last_update_time AS dataset_update_time,
                       t.id AS task_id,
                       t.name AS task_name,
                       t.update_type,
                       t.incremental_field_id,
                       t.incremental_last_value,
                       t.start_time,
                       t.sync_rate,
                       t.cron,
                       t.simple_cron_value,
                       t.simple_cron_type,
                       t.end_time,
                       t.create_time,
                       t.update_time,
                       t.last_exec_time,
                       t.heartbeat_time,
                       t.last_exec_status,
                       t.task_status,
                       t.cache_ready,
                       t.schema_hash,
                       t.full_sync_interval_hours,
                       t.last_full_sync_time,
                       t.verify_enabled,
                       t.last_verify_time,
                       t.last_verify_status,
                       t.last_verify_message,
                       t.last_source_row_count,
                       t.last_cache_row_count,
                       t.cache_expire_hours,
                       t.task_timeout_minutes,
                       t.consecutive_failures,
                       t.failure_warn_threshold,
                       l.start_time AS log_start_time,
                       l.end_time AS log_end_time,
                       l.task_status AS log_task_status,
                       l.info AS log_info,
                       l.trigger_type AS log_trigger_type
                FROM (
                    SELECT DISTINCT c.table_id AS dataset_group_id
                    FROM bi_visualization v
                    INNER JOIN bi_chart c ON c.scene_id = v.id
                    WHERE v.id = ?
                      AND v.delete_flag = 0
                      AND v.node_type = 'leaf'
                """ + visualizationPermissionClause("v") + """
                ) dep
                INNER JOIN dataset_group dg ON dg.id = dep.dataset_group_id
                LEFT JOIN dataset_sync_task t ON t.dataset_group_id = dg.id
                LEFT JOIN dataset_sync_task_log l ON l.id = (
                    %s
                )
                WHERE dg.node_type = 'dataset'
                  AND dg.%s = 1
                ORDER BY dg.name ASC, dg.id ASC
                """.formatted(dialect().limitOne("""
                    SELECT l2.id
                    FROM dataset_sync_task_log l2
                    WHERE l2.dataset_group_id = dg.id
                    ORDER BY l2.start_time DESC, l2.id DESC
                    """), modeColumn()), (rs, rowNum) -> mapTaskCenterRow(rs), args.toArray());
    }

    /**
     * 查询同步任务中心分页列表，包含数据集、任务配置、最近日志和失败原因
     */
    public DatasetSyncTaskPageVO page(Integer page, Integer pageSize, DatasetSyncTaskRequest request) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        DatasetSyncTaskRequest normalized = normalizeRequest(request);
        QueryParts query = taskCenterQuery(normalized);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) " + query.fromWhere, Long.class, query.args.toArray());
        List<Object> args = new ArrayList<>(query.args);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<DatasetSyncTaskDTO> records = jdbcTemplate.query(dialect().limitOffset("""
                SELECT dg.id AS dataset_group_id,
                       dg.name AS dataset_name,
                       dg.last_update_time AS dataset_update_time,
                       t.id AS task_id,
                       t.name AS task_name,
                       t.update_type,
                       t.incremental_field_id,
                       t.incremental_last_value,
                       t.start_time,
                       t.sync_rate,
                       t.cron,
                       t.simple_cron_value,
                       t.simple_cron_type,
                       t.end_time,
                       t.create_time,
                       t.update_time,
                       t.last_exec_time,
                       t.heartbeat_time,
                       t.last_exec_status,
                       t.task_status,
                       t.cache_ready,
                       t.schema_hash,
                       t.full_sync_interval_hours,
                       t.last_full_sync_time,
                       t.verify_enabled,
                       t.last_verify_time,
                       t.last_verify_status,
                       t.last_verify_message,
                       t.last_source_row_count,
                       t.last_cache_row_count,
                       t.cache_expire_hours,
                       t.task_timeout_minutes,
                       t.consecutive_failures,
                       t.failure_warn_threshold,
                       l.start_time AS log_start_time,
                       l.end_time AS log_end_time,
                       l.task_status AS log_task_status,
                       l.info AS log_info,
                       l.trigger_type AS log_trigger_type
                """ + query.fromWhere + """

                ORDER BY COALESCE(t.last_exec_time, t.update_time, dg.last_update_time, dg.create_time, 0) DESC,
                         dg.id DESC
                """, normalizedPageSize, offset), (rs, rowNum) -> mapTaskCenterRow(rs), args.toArray());

        DatasetSyncTaskPageVO vo = new DatasetSyncTaskPageVO();
        vo.setPage(normalizedPage);
        vo.setPageSize(normalizedPageSize);
        vo.setTotal(total == null ? 0 : total);
        vo.setRecords(records);
        return vo;
    }

    /**
     * 保存或更新同步任务配置。增量字段或同步模式变化时会清理增量水位和结构哈希
     */
    @Transactional(rollbackFor = Exception.class)
    public DatasetSyncTaskDTO save(DatasetSyncTaskDTO task) {
        if (task == null || task.getDatasetGroupId() == null) {
            CrestException.throwException("datasetGroupId can not be empty");
        }
        CoreDatasetSyncTask record = normalize(task);
        CoreDatasetSyncTask exists = selectByDatasetGroupId(record.getDatasetGroupId());
        long now = System.currentTimeMillis();
        if (exists == null) {
            record.setId(IDUtils.snowID());
            record.setCreateTime(now);
            record.setUpdateTime(now);
            taskMapper.insert(record);
        } else {
            record.setId(exists.getId());
            record.setCreateTime(exists.getCreateTime());
            record.setUpdateTime(now);
            record.setCacheReady(firstNonNull(exists.getCacheReady(), record.getCacheReady(), 0));
            record.setLastExecTime(firstNonNull(task.getLastExecTime(), exists.getLastExecTime()));
            record.setHeartbeatTime(firstNonNull(task.getHeartbeatTime(), exists.getHeartbeatTime()));
            record.setLastExecStatus(StringUtils.defaultIfBlank(task.getLastExecStatus(), exists.getLastExecStatus()));
            record.setTaskStatus(resolveTaskStatus(record, exists));
            record.setSchemaHash(StringUtils.defaultIfBlank(task.getSchemaHash(), exists.getSchemaHash()));
            record.setLastFullSyncTime(firstNonNull(task.getLastFullSyncTime(), exists.getLastFullSyncTime()));
            record.setLastVerifyTime(firstNonNull(task.getLastVerifyTime(), exists.getLastVerifyTime()));
            record.setLastVerifyStatus(StringUtils.defaultIfBlank(task.getLastVerifyStatus(), exists.getLastVerifyStatus()));
            record.setLastVerifyMessage(StringUtils.defaultIfBlank(task.getLastVerifyMessage(), exists.getLastVerifyMessage()));
            record.setLastSourceRowCount(firstNonNull(task.getLastSourceRowCount(), exists.getLastSourceRowCount()));
            record.setLastCacheRowCount(firstNonNull(task.getLastCacheRowCount(), exists.getLastCacheRowCount()));
            record.setConsecutiveFailures(firstNonNull(task.getConsecutiveFailures(), exists.getConsecutiveFailures(), 0));
            record.setFullSyncIntervalHours(firstNonNull(task.getFullSyncIntervalHours(), exists.getFullSyncIntervalHours(), DatasetSyncUtils.DEFAULT_FULL_SYNC_INTERVAL_HOURS));
            record.setVerifyEnabled(firstNonNull(task.getVerifyEnabled(), exists.getVerifyEnabled(), 1));
            record.setCacheExpireHours(firstNonNull(task.getCacheExpireHours(), exists.getCacheExpireHours(), DatasetSyncUtils.DEFAULT_CACHE_EXPIRE_HOURS));
            record.setTaskTimeoutMinutes(firstNonNull(task.getTaskTimeoutMinutes(), exists.getTaskTimeoutMinutes(), DatasetSyncUtils.DEFAULT_TASK_TIMEOUT_MINUTES));
            record.setFailureWarnThreshold(firstNonNull(task.getFailureWarnThreshold(), exists.getFailureWarnThreshold(), DatasetSyncUtils.DEFAULT_FAILURE_WARN_THRESHOLD));
            boolean incrementalConfigChanged = !Strings.CI.equals(record.getUpdateType(), exists.getUpdateType())
                    || !Objects.equals(record.getIncrementalFieldId(), exists.getIncrementalFieldId());
            if (incrementalConfigChanged) {
                record.setIncrementalLastValue(null);
                record.setSchemaHash(null);
            } else if (StringUtils.isBlank(record.getIncrementalLastValue())) {
                record.setIncrementalLastValue(exists.getIncrementalLastValue());
            }
            taskMapper.updateById(record);
            if (incrementalConfigChanged) {
                clearIncrementalState(record);
            }
        }
        CoreDatasetSyncTask saved = taskMapper.selectById(record.getId());
        refreshSchedule(saved);
        return toDTO(saved);
    }

    /**
     * 读取单个数据集的同步任务配置，未配置时返回空值
     */
    public DatasetSyncTaskDTO task(Long datasetGroupId) {
        CoreDatasetSyncTask task = selectByDatasetGroupId(datasetGroupId);
        return task == null ? null : toDTO(task);
    }

    /**
     * 查询数据集最近二十条同步日志，按开始时间倒序返回给任务详情页
     */
    public List<DatasetSyncLogDTO> logs(Long datasetGroupId) {
        QueryWrapper<CoreDatasetSyncTaskLog> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);
        wrapper.orderByDesc("start_time");
        wrapper.last("limit 20");
        return logMapper.selectList(wrapper).stream().map(this::toLogDTO).toList();
    }

    /**
     * 尝试把任务标记为执行中。返回值表示当前是否已有执行中任务占用该任务
     */
    public synchronized boolean markUnderExecution(CoreDatasetSyncTask task) {
        recoverIfStale(task);
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        wrapper.ne("task_status", TaskStatus.UnderExecution.name());
        long now = System.currentTimeMillis();
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setTaskStatus(TaskStatus.UnderExecution.name());
        record.setLastExecTime(now);
        record.setHeartbeatTime(now);
        return taskMapper.update(record, wrapper) == 0;
    }

    /**
     * 刷新执行中任务心跳，后台恢复逻辑用它判断任务是否已经僵死
     */
    public void touchHeartbeat(Long taskId) {
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setHeartbeatTime(System.currentTimeMillis());
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", taskId);
        wrapper.eq("task_status", TaskStatus.UnderExecution.name());
        taskMapper.update(record, wrapper);
    }

    /**
     * 创建一次同步执行日志，日志初始状态固定为执行中
     */
    public CoreDatasetSyncTaskLog initLog(CoreDatasetSyncTask task, String triggerType) {
        long now = System.currentTimeMillis();
        CoreDatasetSyncTaskLog log = new CoreDatasetSyncTaskLog();
        log.setId(IDUtils.snowID());
        log.setDatasetGroupId(task.getDatasetGroupId());
        log.setTaskId(task.getId());
        log.setUpdateType(task.getUpdateType());
        log.setTableName(DatasetSyncUtils.cacheTableName(task.getDatasetGroupId()));
        log.setTaskStatus(TaskStatus.UnderExecution.name());
        log.setStartTime(now);
        log.setCreateTime(now);
        log.setRowCount(0L);
        log.setTriggerType(triggerType);
        log.setInfo("");
        logMapper.insert(log);
        return log;
    }

    /**
     * 更新同步日志实体，执行器会在过程中写入行数、状态和说明
     */
    public void updateLog(CoreDatasetSyncTaskLog log) {
        logMapper.updateById(log);
    }

    /**
     * 收尾一次同步执行，写回状态、水位、结构哈希、校验结果和下一轮可执行状态
     */
    public void finishTask(CoreDatasetSyncTask task, TaskStatus status, String incrementalLastValue, String schemaHash,
                           boolean fullSync, DatasetSyncUtils.ReconcileResult reconcileResult, Long sourceRowCount, Long cacheRowCount) {
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setLastExecStatus(status.name());
        record.setUpdateTime(System.currentTimeMillis());
        CoreDatasetSyncTask latest = selectById(task.getId());
        if (status == TaskStatus.Completed) {
            record.setCacheReady(1);
            record.setConsecutiveFailures(0);
            if (fullSync) {
                record.setLastFullSyncTime(System.currentTimeMillis());
            }
        } else {
            record.setConsecutiveFailures(Objects.requireNonNullElse(latest == null ? null : latest.getConsecutiveFailures(), 0) + 1);
        }
        if (StringUtils.isNotBlank(incrementalLastValue)) {
            record.setIncrementalLastValue(incrementalLastValue);
        }
        if (StringUtils.isNotBlank(schemaHash)) {
            record.setSchemaHash(schemaHash);
        }
        if (reconcileResult != null) {
            record.setLastVerifyTime(System.currentTimeMillis());
            record.setLastVerifyStatus(reconcileResult.status());
            record.setLastVerifyMessage(reconcileResult.message());
            record.setLastSourceRowCount(sourceRowCount);
            record.setLastCacheRowCount(cacheRowCount);
        }
        if (latest != null && Strings.CI.equalsAny(latest.getTaskStatus(), TaskStatus.Stopped.name(), TaskStatus.Suspend.name())) {
            record.setTaskStatus(latest.getTaskStatus());
        } else if (Strings.CI.equals(task.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())) {
            record.setTaskStatus(TaskStatus.Stopped.name());
        } else {
            record.setTaskStatus(TaskStatus.WaitingForExecution.name());
        }
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        taskMapper.update(record, wrapper);
    }

    /**
     * 应用启动后恢复仍停留在执行中的任务，防止重启后任务中心长期显示运行中
     */
    public void recoverInterruptedTasks() {
        QueryWrapper<CoreDatasetSyncTask> wrapper = new QueryWrapper<>();
        wrapper.eq("task_status", TaskStatus.UnderExecution.name());
        List<CoreDatasetSyncTask> tasks = taskMapper.selectList(wrapper);
        for (CoreDatasetSyncTask task : tasks) {
            markRecovered(task, "应用重启后恢复未完成的数据集同步任务");
        }
    }

    /**
     * 停止同步任务并删除调度，正在执行的任务会在执行完成后保持停止态
     */
    public void stop(Long datasetGroupId) {
        CoreDatasetSyncTask task = selectByDatasetGroupId(datasetGroupId);
        if (task == null) {
            return;
        }
        deleteSchedule(task);
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setTaskStatus(TaskStatus.Stopped.name());
        record.setUpdateTime(System.currentTimeMillis());
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        taskMapper.update(record, wrapper);
    }

    /**
     * 暂停周期同步任务，保留配置和日志但移除调度
     */
    public DatasetSyncTaskDTO pause(Long datasetGroupId) {
        CoreDatasetSyncTask task = selectByDatasetGroupId(datasetGroupId);
        if (task == null) {
            return null;
        }
        deleteSchedule(task);
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setTaskStatus(TaskStatus.Suspend.name());
        record.setUpdateTime(System.currentTimeMillis());
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        taskMapper.update(record, wrapper);
        return task(datasetGroupId);
    }

    /**
     * 恢复同步任务。一次性任务恢复为停止态，周期任务恢复为等待执行并重新注册调度
     */
    public DatasetSyncTaskDTO resume(Long datasetGroupId) {
        CoreDatasetSyncTask task = selectByDatasetGroupId(datasetGroupId);
        if (task == null) {
            return null;
        }
        String nextStatus = Strings.CI.equals(task.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())
                ? TaskStatus.Stopped.name()
                : TaskStatus.WaitingForExecution.name();
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setTaskStatus(nextStatus);
        record.setUpdateTime(System.currentTimeMillis());
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        taskMapper.update(record, wrapper);
        task.setTaskStatus(nextStatus);
        if (!Strings.CI.equals(task.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())) {
            refreshSchedule(task);
        }
        return task(datasetGroupId);
    }

    /**
     * 删除数据集对应的同步任务、日志、调度和缓存表，供数据集删除流程调用
     */
    public void deleteByDatasetGroupId(Long datasetGroupId) {
        CoreDatasetSyncTask task = selectByDatasetGroupId(datasetGroupId);
        if (task != null) {
            CoreDatasetSyncTask record = new CoreDatasetSyncTask();
            record.setTaskStatus(TaskStatus.Stopped.name());
            record.setUpdateTime(System.currentTimeMillis());
            UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
            wrapper.eq("id", task.getId());
            taskMapper.update(record, wrapper);
            deleteSchedule(task);
        }
        QueryWrapper<CoreDatasetSyncTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.eq("dataset_group_id", datasetGroupId);
        taskMapper.delete(taskWrapper);
        QueryWrapper<CoreDatasetSyncTaskLog> logWrapper = new QueryWrapper<>();
        logWrapper.eq("dataset_group_id", datasetGroupId);
        logMapper.delete(logWrapper);
        dropCacheTable(DatasetSyncUtils.cacheTableName(datasetGroupId));
        dropCacheTable(DatasetSyncUtils.tmpCacheTableName(datasetGroupId));
    }

    /**
     * 根据任务同步频率刷新调度。一次性任务不保留 Quartz 调度
     */
    public void refreshSchedule(CoreDatasetSyncTask task) {
        if (Strings.CI.equals(task.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())) {
            deleteSchedule(task);
            return;
        }
        addSchedule(task);
    }

    /**
     * 判断启动恢复时是否需要重建调度，停止和暂停状态不会自动恢复
     */
    public boolean shouldRestoreSchedule(CoreDatasetSyncTask task) {
        if (task == null || Strings.CI.equals(task.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())) {
            return false;
        }
        return !Strings.CI.equalsAny(task.getTaskStatus(), TaskStatus.Stopped.name(), TaskStatus.Suspend.name());
    }

    /**
     * 注册或更新 Quartz 同步任务，结束时间已过时会直接清理调度
     */
    public void addSchedule(CoreDatasetSyncTask task) {
        Date endTime = null;
        if (task.getEndTime() != null && task.getEndTime() > 0) {
            endTime = new Date(task.getEndTime());
            if (endTime.before(new Date())) {
                deleteSchedule(task);
                return;
            }
        }
        String taskId = task.getId().toString();
        String datasetGroupId = task.getDatasetGroupId().toString();
        scheduleManager.addOrUpdateCronJob(
                new JobKey(taskId, jobGroup(datasetGroupId)),
                new TriggerKey(taskId, jobGroup(datasetGroupId)),
                DatasetSyncJob.class,
                task.getCron(),
                new Date(Objects.requireNonNullElse(task.getStartTime(), System.currentTimeMillis())),
                endTime,
                scheduleManager.getDefaultJobDataMap(datasetGroupId, task.getCron(), taskId, task.getUpdateType())
        );
    }

    /**
     * 删除同步任务对应的 Quartz job 和 trigger，不存在时保持幂等
     */
    public void deleteSchedule(CoreDatasetSyncTask task) {
        String taskId = task.getId().toString();
        String datasetGroupId = task.getDatasetGroupId().toString();
        JobKey jobKey = new JobKey(taskId, jobGroup(datasetGroupId));
        if (!scheduleManager.exist(jobKey)) {
            return;
        }
        scheduleManager.removeJob(jobKey, new TriggerKey(taskId, jobGroup(datasetGroupId)));
    }

    /**
     * 检查数据集缓存表是否存在，使用空查询验证引擎侧表可访问
     */
    public boolean cacheTableExists(Long datasetGroupId) {
        try {
            CoreEngine engine = engineManage.info();
            DatasourceConfiguration.DatasourceType engineType = DatasourceConfiguration.DatasourceType.valueOf(engine.getType());
            EngineRequest request = new EngineRequest();
            request.setEngine(engine);
            request.setQuery("SELECT 1 FROM " + DatasetSyncUtils.quote(
                    DatasetSyncUtils.cacheTableName(datasetGroupId),
                    engineType.getPrefix(),
                    engineType.getSuffix()
            ) + " WHERE 1 = 0");
            calciteProvider.exec(request);
            return true;
        } catch (Exception e) {
            LogUtil.warn("Dataset sync cache table is not available: " + datasetGroupId + ", " + e.getMessage());
            return false;
        }
    }

    /**
     * 规范化同步任务配置，补齐默认值并清理不适用的增量字段
     */
    private CoreDatasetSyncTask normalize(DatasetSyncTaskDTO task) {
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        BeanUtils.copyBean(record, task);
        if (StringUtils.isBlank(record.getName())) {
            record.setName("dataset_sync_" + task.getDatasetGroupId());
        }
        if (StringUtils.isBlank(record.getUpdateType())) {
            record.setUpdateType(DEFAULT_UPDATE_TYPE);
        }
        if (!Strings.CI.equals(record.getUpdateType(), "add_scope")) {
            record.setIncrementalFieldId(null);
            record.setIncrementalLastValue(null);
        }
        if (StringUtils.isBlank(record.getSyncRate())) {
            record.setSyncRate(DatasourceTaskServer.ScheduleType.RIGHTNOW.name());
        }
        if (record.getStartTime() == null || record.getStartTime() <= 0) {
            record.setStartTime(System.currentTimeMillis());
        }
        if (Strings.CI.equals(record.getSyncRate(), DatasourceTaskServer.ScheduleType.SIMPLE_CRON.name())) {
            record.setCron(simpleCron(record));
        }
        if (!Strings.CI.equals(record.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())
                && StringUtils.isBlank(record.getCron())) {
            CrestException.throwException("cron can not be empty");
        }
        if (StringUtils.isBlank(record.getTaskStatus())) {
            record.setTaskStatus(Strings.CI.equals(record.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())
                    ? TaskStatus.Stopped.name()
                    : TaskStatus.WaitingForExecution.name());
        }
        if (record.getCacheReady() == null) {
            record.setCacheReady(0);
        }
        if (record.getFullSyncIntervalHours() == null) {
            record.setFullSyncIntervalHours(DatasetSyncUtils.DEFAULT_FULL_SYNC_INTERVAL_HOURS);
        }
        if (record.getVerifyEnabled() == null) {
            record.setVerifyEnabled(1);
        }
        if (record.getCacheExpireHours() == null) {
            record.setCacheExpireHours(DatasetSyncUtils.DEFAULT_CACHE_EXPIRE_HOURS);
        }
        if (record.getTaskTimeoutMinutes() == null) {
            record.setTaskTimeoutMinutes(DatasetSyncUtils.DEFAULT_TASK_TIMEOUT_MINUTES);
        }
        if (record.getConsecutiveFailures() == null) {
            record.setConsecutiveFailures(0);
        }
        if (record.getFailureWarnThreshold() == null) {
            record.setFailureWarnThreshold(DatasetSyncUtils.DEFAULT_FAILURE_WARN_THRESHOLD);
        }
        return record;
    }

    /**
     * 将简单周期配置转换为 Quartz cron 表达式，并限制周期值在可执行范围内
     */
    private String simpleCron(CoreDatasetSyncTask task) {
        long value = Objects.requireNonNullElse(task.getSimpleCronValue(), 30L);
        String type = StringUtils.defaultIfBlank(task.getSimpleCronType(), "minute");
        if (Strings.CI.equals(type, "hour")) {
            value = Math.max(1, Math.min(value, 23));
            return "0 0 0/" + value + " * * ? *";
        }
        if (Strings.CI.equals(type, "day")) {
            value = Math.max(1, Math.min(value, 31));
            return "0 0 0 1/" + value + " * ? *";
        }
        value = Math.max(1, Math.min(value, 59));
        return "0 0/" + value + " * * * ? *";
    }

    /**
     * 将任务实体转换为接口对象，并补充缓存过期和失败预警状态
     */
    private DatasetSyncTaskDTO toDTO(CoreDatasetSyncTask task) {
        DatasetSyncTaskDTO dto = new DatasetSyncTaskDTO();
        BeanUtils.copyBean(dto, task);
        long now = System.currentTimeMillis();
        dto.setCacheExpired(DatasetSyncUtils.isCacheExpired(task, now));
        dto.setFailureWarned(DatasetSyncUtils.isFailureWarned(task));
        return dto;
    }

    /**
     * 规范化任务中心查询条件，空字符串统一转为空值
     */
    private DatasetSyncTaskRequest normalizeRequest(DatasetSyncTaskRequest request) {
        DatasetSyncTaskRequest normalized = request == null ? new DatasetSyncTaskRequest() : request;
        normalized.setKeyword(StringUtils.trimToNull(normalized.getKeyword()));
        normalized.setTaskStatus(StringUtils.trimToNull(normalized.getTaskStatus()));
        normalized.setSyncRate(StringUtils.trimToNull(normalized.getSyncRate()));
        normalized.setUpdateType(StringUtils.trimToNull(normalized.getUpdateType()));
        return normalized;
    }

    /**
     * 组装任务中心查询的 FROM、WHERE 和参数列表，权限范围在此处合入
     */
    private QueryParts taskCenterQuery(DatasetSyncTaskRequest request) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                FROM dataset_group dg
                LEFT JOIN dataset_sync_task t ON t.dataset_group_id = dg.id
                LEFT JOIN dataset_sync_task_log l ON l.id = (
                    %s
                )
                WHERE dg.node_type = 'dataset'
                  AND dg.%s = 1
                """.formatted(dialect().limitOne("""
                    SELECT l2.id
                    FROM dataset_sync_task_log l2
                    WHERE l2.dataset_group_id = dg.id
                    ORDER BY l2.start_time DESC, l2.id DESC
                    """), modeColumn()));
        String permissionScope = platformPermissionManage == null
                ? null
                : platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null);
        if (StringUtils.isNotBlank(permissionScope)) {
            sql.append(" AND ").append(permissionScope);
        }
        if (StringUtils.isNotBlank(request.getKeyword())) {
            sql.append(" AND (")
                    .append(dialect().caseInsensitiveLike("dg.name", "?"))
                    .append(" OR ")
                    .append(dialect().caseInsensitiveLike(dialect().stringCast("dg.id"), "?"))
                    .append(")");
            args.add(request.getKeyword());
            args.add(request.getKeyword());
        }
        if (StringUtils.isNotBlank(request.getTaskStatus())) {
            sql.append(" AND COALESCE(t.task_status, ?) = ?");
            args.add(TaskStatus.WaitingForExecution.name());
            args.add(request.getTaskStatus());
        }
        if (StringUtils.isNotBlank(request.getSyncRate())) {
            sql.append(" AND COALESCE(t.sync_rate, ?) = ?");
            args.add(DatasourceTaskServer.ScheduleType.RIGHTNOW.name());
            args.add(request.getSyncRate());
        }
        if (StringUtils.isNotBlank(request.getUpdateType())) {
            sql.append(" AND COALESCE(t.update_type, ?) = ?");
            args.add(DEFAULT_UPDATE_TYPE);
            args.add(request.getUpdateType());
        }
        return new QueryParts(sql.toString(), args);
    }

    /**
     * 构造可视化依赖查询的权限条件，仪表板和数据大屏使用不同资源类型判断
     */
    private String visualizationPermissionClause(String alias) {
        if (platformPermissionManage == null) {
            return "";
        }
        String screenScope = platformPermissionManage.resourceScopeSql("screen", alias + ".id", alias + ".create_by", alias + ".org_id");
        String panelScope = platformPermissionManage.resourceScopeSql("panel", alias + ".id", alias + ".create_by", alias + ".org_id");
        if (StringUtils.isBlank(screenScope) && StringUtils.isBlank(panelScope)) {
            return "";
        }
        String screenClause = StringUtils.isBlank(screenScope) ? "1 = 1" : screenScope;
        String panelClause = StringUtils.isBlank(panelScope) ? "1 = 1" : panelScope;
        return """

                      AND ((%s.type = 'dataV' AND %s)
                        OR (%s.type <> 'dataV' AND %s))
                """.formatted(alias, screenClause, alias, panelClause);
    }

    /**
     * 将任务中心查询结果行映射为 DTO，未配置任务的数据集会使用默认任务视图
     */
    private DatasetSyncTaskDTO mapTaskCenterRow(ResultSet rs) throws SQLException {
        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        task.setId(longOrNull(rs, "task_id"));
        task.setDatasetGroupId(rs.getLong("dataset_group_id"));
        task.setName(StringUtils.defaultIfBlank(rs.getString("task_name"), rs.getString("dataset_name")));
        task.setUpdateType(StringUtils.defaultIfBlank(rs.getString("update_type"), DEFAULT_UPDATE_TYPE));
        task.setIncrementalFieldId(longOrNull(rs, "incremental_field_id"));
        task.setIncrementalLastValue(rs.getString("incremental_last_value"));
        task.setStartTime(longOrNull(rs, "start_time"));
        task.setSyncRate(StringUtils.defaultIfBlank(rs.getString("sync_rate"), DatasourceTaskServer.ScheduleType.RIGHTNOW.name()));
        task.setCron(rs.getString("cron"));
        task.setSimpleCronValue(longOrNull(rs, "simple_cron_value"));
        task.setSimpleCronType(rs.getString("simple_cron_type"));
        task.setEndTime(longOrNull(rs, "end_time"));
        task.setCreateTime(longOrNull(rs, "create_time"));
        task.setUpdateTime(firstNonNull(longOrNull(rs, "update_time"), longOrNull(rs, "dataset_update_time")));
        task.setLastExecTime(longOrNull(rs, "last_exec_time"));
        task.setHeartbeatTime(longOrNull(rs, "heartbeat_time"));
        task.setLastExecStatus(rs.getString("last_exec_status"));
        task.setTaskStatus(StringUtils.defaultIfBlank(rs.getString("task_status"), TaskStatus.WaitingForExecution.name()));
        task.setCacheReady(intOrNull(rs, "cache_ready"));
        task.setSchemaHash(rs.getString("schema_hash"));
        task.setFullSyncIntervalHours(intOrNull(rs, "full_sync_interval_hours"));
        task.setLastFullSyncTime(longOrNull(rs, "last_full_sync_time"));
        task.setVerifyEnabled(intOrNull(rs, "verify_enabled"));
        task.setLastVerifyTime(longOrNull(rs, "last_verify_time"));
        task.setLastVerifyStatus(rs.getString("last_verify_status"));
        task.setLastVerifyMessage(rs.getString("last_verify_message"));
        task.setLastSourceRowCount(longOrNull(rs, "last_source_row_count"));
        task.setLastCacheRowCount(longOrNull(rs, "last_cache_row_count"));
        task.setCacheExpireHours(intOrNull(rs, "cache_expire_hours"));
        task.setTaskTimeoutMinutes(intOrNull(rs, "task_timeout_minutes"));
        task.setConsecutiveFailures(intOrNull(rs, "consecutive_failures"));
        task.setFailureWarnThreshold(intOrNull(rs, "failure_warn_threshold"));

        DatasetSyncTaskDTO dto = toDTO(task);
        dto.setDatasetName(rs.getString("dataset_name"));
        dto.setLastLogStartTime(longOrNull(rs, "log_start_time"));
        dto.setLastLogEndTime(longOrNull(rs, "log_end_time"));
        dto.setLastLogStatus(rs.getString("log_task_status"));
        dto.setLastLogInfo(rs.getString("log_info"));
        dto.setLastTriggerType(rs.getString("log_trigger_type"));
        dto.setDurationMillis(durationMillis(dto.getLastLogStartTime(), dto.getLastLogEndTime(), dto.getTaskStatus()));
        dto.setFailureReason(failureReason(dto));
        return dto;
    }

    /**
     * 计算最近一次同步耗时。执行中任务使用当前时间作为临时结束时间
     */
    private Long durationMillis(Long startTime, Long endTime, String taskStatus) {
        if (startTime == null || startTime <= 0) {
            return null;
        }
        long effectiveEndTime = endTime == null || endTime <= 0
                ? (Strings.CI.equals(taskStatus, TaskStatus.UnderExecution.name()) ? System.currentTimeMillis() : 0)
                : endTime;
        if (effectiveEndTime <= 0 || effectiveEndTime < startTime) {
            return null;
        }
        return effectiveEndTime - startTime;
    }

    /**
     * 提取任务中心可展示的失败原因，优先使用最近日志，其次使用校验消息
     */
    private String failureReason(DatasetSyncTaskDTO task) {
        if (!Strings.CI.equalsAny(task.getLastExecStatus(), TaskStatus.Error.name(), TaskStatus.Warning.name())
                && !Strings.CI.equalsAny(task.getLastLogStatus(), TaskStatus.Error.name(), TaskStatus.Warning.name())
                && !Boolean.TRUE.equals(task.getFailureWarned())
                && !Strings.CI.equals(task.getLastVerifyStatus(), "WARNING")) {
            return null;
        }
        return StringUtils.defaultIfBlank(task.getLastLogInfo(), task.getLastVerifyMessage());
    }

    /**
     * 从结果集中读取可空 long，避免 JDBC 默认值 0 混淆真实空值
     */
    private Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 从结果集中读取可空 int，避免 JDBC 默认值 0 混淆真实空值
     */
    private Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 规范化页码，最小页码固定为 1
     */
    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    /**
     * 规范化分页大小，限制最大页容量避免任务中心查询过重
     */
    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }

    /**
     * 保存配置时解析任务状态，执行中的任务保持执行中，一次性任务保存后保持停止态
     */
    private String resolveTaskStatus(CoreDatasetSyncTask record, CoreDatasetSyncTask exists) {
        if (Strings.CI.equals(exists.getTaskStatus(), TaskStatus.UnderExecution.name())) {
            return TaskStatus.UnderExecution.name();
        }
        if (Strings.CI.equals(record.getSyncRate(), DatasourceTaskServer.ScheduleType.RIGHTNOW.name())) {
            return TaskStatus.Stopped.name();
        }
        return TaskStatus.WaitingForExecution.name();
    }

    /**
     * 清理增量同步水位和结构哈希，避免配置变化后继续沿用旧缓存状态
     */
    private void clearIncrementalState(CoreDatasetSyncTask task) {
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId())
                .set("incremental_last_value", null)
                .set("schema_hash", null);
        if (!Strings.CI.equals(task.getUpdateType(), "add_scope")) {
            wrapper.set("incremental_field_id", null);
        }
        taskMapper.update(new CoreDatasetSyncTask(), wrapper);
    }

    /**
     * 在两个候选值中返回第一个非空值，用于保留历史任务状态
     */
    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    /**
     * 在两个候选值均为空时返回默认值
     */
    private <T> T firstNonNull(T preferred, T fallback, T defaultValue) {
        T value = firstNonNull(preferred, fallback);
        return value != null ? value : defaultValue;
    }

    /**
     * 将同步日志实体转换为接口对象
     */
    private DatasetSyncLogDTO toLogDTO(CoreDatasetSyncTaskLog log) {
        DatasetSyncLogDTO dto = new DatasetSyncLogDTO();
        BeanUtils.copyBean(dto, log);
        return dto;
    }

    /**
     * 生成数据集同步任务的 Quartz 分组名，按数据集隔离调度键
     */
    private String jobGroup(String datasetGroupId) {
        return JOB_GROUP_PREFIX + datasetGroupId;
    }

    // 数据集同步任务需要按元数据库方言引用保留字段
    private MetadataDbDialect dialect() {
        return MetadataDbDialects.current(environment);
    }

    // mode 在 Oracle 兼容模式下是保留字，统一通过方言引用
    private String modeColumn() {
        return dialect().quoteIdentifier("MODE");
    }

    /**
     * 任务中心查询片段和参数，调用方负责拼接 SELECT、排序和分页
     */
    private record QueryParts(String fromWhere, List<Object> args) {
    }

    /**
     * 执行前检查任务是否长时间无心跳，超过阈值时先恢复再允许重新抢占
     */
    private void recoverIfStale(CoreDatasetSyncTask task) {
        CoreDatasetSyncTask latest = selectById(task.getId());
        if (latest == null || !Strings.CI.equals(latest.getTaskStatus(), TaskStatus.UnderExecution.name())) {
            return;
        }
        Long runningTime = latest.getHeartbeatTime() != null ? latest.getHeartbeatTime() : latest.getLastExecTime();
        long runningAt = runningTime == null ? 0L : runningTime;
        if (runningAt == 0 || System.currentTimeMillis() - runningAt <= RUNNING_TASK_STALE_MILLIS) {
            return;
        }
        markRecovered(latest, "数据集同步任务超过运行心跳阈值，已自动恢复");
    }

    /**
     * 将中断或僵死任务恢复为等待执行，并把最后一条执行中日志标记为失败
     */
    private void markRecovered(CoreDatasetSyncTask task, String message) {
        CoreDatasetSyncTask record = new CoreDatasetSyncTask();
        record.setTaskStatus(TaskStatus.WaitingForExecution.name());
        record.setLastExecStatus(TaskStatus.Error.name());
        record.setConsecutiveFailures(Objects.requireNonNullElse(task.getConsecutiveFailures(), 0) + 1);
        record.setUpdateTime(System.currentTimeMillis());
        UpdateWrapper<CoreDatasetSyncTask> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", task.getId());
        wrapper.eq("task_status", TaskStatus.UnderExecution.name());
        taskMapper.update(record, wrapper);

        CoreDatasetSyncTaskLog logRecord = new CoreDatasetSyncTaskLog();
        logRecord.setTaskStatus(TaskStatus.Error.name());
        logRecord.setEndTime(System.currentTimeMillis());
        logRecord.setInfo(message);
        UpdateWrapper<CoreDatasetSyncTaskLog> logWrapper = new UpdateWrapper<>();
        logWrapper.eq("task_id", task.getId());
        logWrapper.eq("task_status", TaskStatus.UnderExecution.name());
        logMapper.update(logRecord, logWrapper);
    }

    /**
     * 删除同步缓存表，失败只记录告警，避免影响数据集删除主流程
     */
    private void dropCacheTable(String tableName) {
        try {
            CoreEngine engine = engineManage.info();
            EngineProvider engineProvider = ProviderUtil.getEngineProvider(engine.getType());
            EngineRequest request = new EngineRequest();
            request.setEngine(engine);
            request.setQuery(engineProvider.dropTable(tableName));
            calciteProvider.exec(request);
        } catch (Exception e) {
            LogUtil.warn("Drop dataset sync cache table failed: " + tableName + ", " + e.getMessage());
        }
    }
}
