package io.crest.datasource.server;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.crest.commons.constants.TaskStatus;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.entity.CoreDatasourceTask;
import io.crest.datasource.dao.auto.entity.CoreDatasourceTaskLog;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceTaskLogMapper;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceTaskMapper;
import io.crest.datasource.dto.CoreDatasourceTaskDTO;
import io.crest.datasource.dao.ext.mapper.ExtDatasourceTaskMapper;
import io.crest.datasource.manage.DatasourceSyncManage;
import io.crest.utils.IDUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
// 管理数据源同步任务、任务日志和任务状态流转
public class DatasourceTaskServer {

    @Resource
    private CoreDatasourceTaskMapper datasourceTaskMapper;
    @Resource
    private CoreDatasourceMapper coreDatasourceMapper;
    @Resource
    private ExtDatasourceTaskMapper extDatasourceTaskMapper;
    @Resource
    private CoreDatasourceTaskLogMapper coreDatasourceTaskLogMapper;
    @Resource
    private DatasourceSyncManage datasourceSyncManage;


    // 按任务 ID 查询数据源同步任务
    public CoreDatasourceTask selectById(Long taskId) {
        return datasourceTaskMapper.selectById(taskId);
    }

    // 查询所有数据源同步任务
    public List<CoreDatasourceTask> listAll() {
        return datasourceTaskMapper.selectList(null);
    }

    // 按数据源 ID 查询同步任务
    public CoreDatasourceTask selectByDSId(Long dsId) {
        QueryWrapper<CoreDatasourceTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ds_id", dsId);
        List<CoreDatasourceTask> coreDatasourceTasks = datasourceTaskMapper.selectList(queryWrapper);
        return CollectionUtils.isEmpty(coreDatasourceTasks) ? new CoreDatasourceTask() : coreDatasourceTasks.get(0);
    }

    // 查询指定数据源表的最近同步日志
    public CoreDatasourceTaskLog lastSyncLogForTable(Long dsId, String tableName) {
        List<CoreDatasourceTaskLog> coreDatasourceTaskLogs = new ArrayList<>();
        QueryWrapper<CoreDatasourceTaskLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ds_id", dsId);
        queryWrapper.eq("table_name", tableName);
        queryWrapper.orderByDesc("start_time");
        queryWrapper.last("limit 1");
        List<CoreDatasourceTaskLog> logs = coreDatasourceTaskLogMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(logs)) {
            return logs.get(0);
        } else {
            return null;
        }
    }

    // 删除数据源下的同步任务和调度
    public void deleteByDSId(Long dsId) {
        QueryWrapper<CoreDatasourceTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ds_id", dsId);
        List<CoreDatasourceTask> coreDatasourceTasks = datasourceTaskMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(coreDatasourceTasks)) {
            datasourceSyncManage.deleteSchedule(coreDatasourceTasks.get(0));
        }
        datasourceTaskMapper.delete(queryWrapper);
    }

    // 新增数据源同步任务
    public void insert(CoreDatasourceTask coreDatasourceTask) {
        coreDatasourceTask.setId(IDUtils.snowID());
        datasourceTaskMapper.insert(coreDatasourceTask);
    }

    // 按任务 ID 删除同步任务
    public void delete(Long id) {
        datasourceTaskMapper.deleteById(id);
    }

    // 新增或更新同步任务
    public void update(CoreDatasourceTask coreDatasourceTask) {
        if (coreDatasourceTask.getId() == null) {
            datasourceTaskMapper.insert(coreDatasourceTask);
        } else {
            UpdateWrapper<CoreDatasourceTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", coreDatasourceTask.getId());
            datasourceTaskMapper.updateById(coreDatasourceTask);
        }

    }

    // 批量重置数据源同步任务状态
    public void updateByDsIds(List<Long> dsIds) {
        UpdateWrapper<CoreDatasourceTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("ds_id", dsIds);
        CoreDatasourceTask record = new CoreDatasourceTask();
        record.setTaskStatus(TaskStatus.WaitingForExecution.name());
        datasourceTaskMapper.update(record, updateWrapper);
    }

    // 根据结束时间和下一次触发时间判断任务是否停止
    public void checkTaskIsStopped(CoreDatasourceTask coreDatasourceTask) {
        if (coreDatasourceTask.getEndTime() != null && coreDatasourceTask.getEndTime() > 0) {
            List<CoreDatasourceTaskDTO> dataSetTaskDTOS = taskWithTriggers(coreDatasourceTask.getId());
            if (CollectionUtils.isEmpty(dataSetTaskDTOS)) {
                return;
            }
            UpdateWrapper<CoreDatasourceTask> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", coreDatasourceTask.getId());
            CoreDatasourceTask datasourceTask = new CoreDatasourceTask();
            if (dataSetTaskDTOS.get(0).getNextExecTime() == null || dataSetTaskDTOS.get(0).getNextExecTime() <= 0) {
                datasourceTask.setTaskStatus(TaskStatus.Stopped.name());
                datasourceTaskMapper.update(datasourceTask, updateWrapper);
            }
            if (dataSetTaskDTOS.get(0).getNextExecTime() != null && dataSetTaskDTOS.get(0).getNextExecTime() > coreDatasourceTask.getEndTime()) {
                datasourceTask.setTaskStatus(TaskStatus.Stopped.name());
                datasourceTaskMapper.update(datasourceTask, updateWrapper);
            }
        }
    }

    // 查询任务关联的调度触发器
    public List<CoreDatasourceTaskDTO> taskWithTriggers(Long taskId) {
        QueryWrapper<CoreDatasourceTaskDTO> wrapper = new QueryWrapper<>();
        wrapper.eq("QRTZ_TRIGGERS.TRIGGER_NAME", String.valueOf(taskId));
        return extDatasourceTaskMapper.taskWithTriggers(wrapper);
    }

    // 尝试标记任务执行中并判断是否已有执行任务
    public synchronized boolean existUnderExecutionTask(Long datasourceId, Long taskId) {
        UpdateWrapper<CoreDatasource> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", datasourceId);
        updateWrapper.ne("task_status", TaskStatus.UnderExecution.name());
        CoreDatasource coreDatasource = new CoreDatasource();
        coreDatasource.setTaskStatus(TaskStatus.UnderExecution.name());
        Boolean existSyncTask = coreDatasourceMapper.update(coreDatasource, updateWrapper) == 0;
        if (!existSyncTask) {
            UpdateWrapper<CoreDatasourceTask> updateTaskWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", taskId);
            CoreDatasourceTask record = new CoreDatasourceTask();
            record.setTaskStatus(TaskStatus.UnderExecution.name());
            record.setLastExecTime(System.currentTimeMillis());
            datasourceTaskMapper.update(record, updateTaskWrapper);
        }
        return existSyncTask;
    }

    // 初始化数据源同步任务日志
    public CoreDatasourceTaskLog initTaskLog(Long datasourceId, Long taskId, String tableName, String triggerType) {
        Long startTime = System.currentTimeMillis();
        CoreDatasourceTaskLog coreDatasourceTaskLog = new CoreDatasourceTaskLog();
        coreDatasourceTaskLog.setId(IDUtils.snowID());
        coreDatasourceTaskLog.setDsId(datasourceId);
        coreDatasourceTaskLog.setTaskId(taskId);
        coreDatasourceTaskLog.setTaskStatus(TaskStatus.UnderExecution.name());
        coreDatasourceTaskLog.setTriggerType(triggerType);
        coreDatasourceTaskLog.setStartTime(startTime);
        coreDatasourceTaskLog.setCreateTime(startTime);
        coreDatasourceTaskLog.setTableName(tableName);
        coreDatasourceTaskLog.setInfo("");
        coreDatasourceTaskLogMapper.insert(coreDatasourceTaskLog);
        return coreDatasourceTaskLog;
    }

    // 保存同步任务日志
    public void saveLog(CoreDatasourceTaskLog coreDatasourceTaskLog) {
        coreDatasourceTaskLogMapper.updateById(coreDatasourceTaskLog);
    }

    // 根据调度类型和触发器状态更新任务状态
    public void updateTaskStatus(CoreDatasourceTask coreDatasourceTask) {
        CoreDatasourceTask record = new CoreDatasourceTask();
        if (coreDatasourceTask.getSyncRate().equalsIgnoreCase(ScheduleType.RIGHTNOW.name())) {
            record.setTaskStatus(TaskStatus.Stopped.name());
        } else {
            if (coreDatasourceTask.getEndTime() != null && coreDatasourceTask.getEndTime() > 0) {
                List<CoreDatasourceTaskDTO> dataSetTaskDTOS = taskWithTriggers(coreDatasourceTask.getId());
                if (CollectionUtils.isEmpty(dataSetTaskDTOS)) {
                    return;
                }
                if (dataSetTaskDTOS.get(0).getNextExecTime() == null || dataSetTaskDTOS.get(0).getNextExecTime() <= 0) {
                    record.setTaskStatus(TaskStatus.Stopped.name());
                } else {
                    record.setTaskStatus(TaskStatus.WaitingForExecution.name());
                }
            } else {
                record.setTaskStatus(TaskStatus.WaitingForExecution.name());
            }
        }

        UpdateWrapper<CoreDatasourceTask> updateTaskWrapper = new UpdateWrapper<>();
        updateTaskWrapper.eq("id", coreDatasourceTask.getId());
        datasourceTaskMapper.update(record, updateTaskWrapper);
    }

    // 清理过期数据源同步日志
    public void cleanLog() {
        long expTime = Long.parseLong("30") * 24L * 3600L * 1000L;
        long threshold = System.currentTimeMillis() - expTime;
        QueryWrapper<CoreDatasourceTaskLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.lt("start_time", threshold);
        coreDatasourceTaskLogMapper.delete(queryWrapper);
    }


    // 数据源同步任务调度类型
    public enum ScheduleType {
        CRON, RIGHTNOW, SIMPLE_CRON, MANUAL
    }
}
