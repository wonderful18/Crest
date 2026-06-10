package io.crest.dataset.sync;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.crest.api.dataset.dto.DatasetSyncTaskDTO;
import io.crest.commons.constants.TaskStatus;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTask;
import io.crest.dataset.dao.auto.mapper.CoreDatasetSyncTaskMapper;
import io.crest.datasource.server.DatasourceTaskServer;
import io.crest.job.schedule.ScheduleManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobDataMap;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetSyncTaskManageTest {

    @Test
    void saveScheduledTaskHandlesNullRuntimeFieldsFromExistingTask() {
        DatasetSyncTaskManage manage = new DatasetSyncTaskManage();
        CoreDatasetSyncTaskMapper taskMapper = mock(CoreDatasetSyncTaskMapper.class);
        ScheduleManager scheduleManager = mock(ScheduleManager.class);
        ReflectionTestUtils.setField(manage, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(manage, "scheduleManager", scheduleManager);

        CoreDatasetSyncTask exists = new CoreDatasetSyncTask();
        exists.setId(1262155861866975232L);
        exists.setDatasetGroupId(982004L);
        exists.setName("角色分布透视");
        exists.setUpdateType("all_scope");
        exists.setSyncRate(DatasourceTaskServer.ScheduleType.RIGHTNOW.name());
        exists.setCreateTime(1781080000000L);
        exists.setTaskStatus(TaskStatus.Stopped.name());

        when(taskMapper.selectOne(any())).thenReturn(exists);
        CoreDatasetSyncTask[] updated = new CoreDatasetSyncTask[1];
        when(taskMapper.updateById(any())).thenAnswer(invocation -> {
            updated[0] = invocation.getArgument(0);
            return 1;
        });
        when(taskMapper.selectById(1262155861866975232L)).thenAnswer(invocation -> updated[0]);
        when(scheduleManager.getDefaultJobDataMap(eq("982004"), any(), eq("1262155861866975232"), any()))
                .thenReturn(new JobDataMap());

        DatasetSyncTaskDTO request = new DatasetSyncTaskDTO();
        request.setDatasetGroupId(982004L);
        request.setName("角色分布透视");
        request.setUpdateType("add_scope");
        request.setIncrementalFieldId(984000405L);
        request.setSyncRate(DatasourceTaskServer.ScheduleType.SIMPLE_CRON.name());
        request.setSimpleCronValue(30L);
        request.setSimpleCronType("minute");
        request.setFullSyncIntervalHours(24);
        request.setVerifyEnabled(1);
        request.setCacheExpireHours(26);
        request.setTaskTimeoutMinutes(360);
        request.setFailureWarnThreshold(1);

        DatasetSyncTaskDTO saved = manage.save(request);

        ArgumentCaptor<CoreDatasetSyncTask> recordCaptor = ArgumentCaptor.forClass(CoreDatasetSyncTask.class);
        verify(taskMapper).updateById(recordCaptor.capture());
        CoreDatasetSyncTask record = recordCaptor.getValue();
        assertEquals(TaskStatus.WaitingForExecution.name(), record.getTaskStatus());
        assertEquals("add_scope", record.getUpdateType());
        assertEquals(984000405L, record.getIncrementalFieldId());
        assertFalse(record.getCron().isBlank());
        assertEquals(TaskStatus.WaitingForExecution.name(), saved.getTaskStatus());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void saveAllScopeClearsStaleIncrementalFields() {
        DatasetSyncTaskManage manage = new DatasetSyncTaskManage();
        CoreDatasetSyncTaskMapper taskMapper = mock(CoreDatasetSyncTaskMapper.class);
        ScheduleManager scheduleManager = mock(ScheduleManager.class);
        ReflectionTestUtils.setField(manage, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(manage, "scheduleManager", scheduleManager);

        CoreDatasetSyncTask exists = new CoreDatasetSyncTask();
        exists.setId(1262155861866975232L);
        exists.setDatasetGroupId(982004L);
        exists.setName("角色分布透视");
        exists.setUpdateType("add_scope");
        exists.setIncrementalFieldId(984000405L);
        exists.setIncrementalLastValue("79");
        exists.setSchemaHash("old-schema");
        exists.setSyncRate(DatasourceTaskServer.ScheduleType.SIMPLE_CRON.name());
        exists.setCron("0 0/30 * * * ? *");
        exists.setCreateTime(1781080000000L);
        exists.setTaskStatus(TaskStatus.WaitingForExecution.name());
        exists.setCacheReady(1);

        when(taskMapper.selectOne(any())).thenReturn(exists);
        CoreDatasetSyncTask[] updated = new CoreDatasetSyncTask[1];
        when(taskMapper.updateById(any())).thenAnswer(invocation -> {
            updated[0] = invocation.getArgument(0);
            return 1;
        });
        when(taskMapper.selectById(1262155861866975232L)).thenAnswer(invocation -> updated[0]);
        when(scheduleManager.getDefaultJobDataMap(eq("982004"), any(), eq("1262155861866975232"), any()))
                .thenReturn(new JobDataMap());

        DatasetSyncTaskDTO request = new DatasetSyncTaskDTO();
        request.setDatasetGroupId(982004L);
        request.setName("角色分布透视");
        request.setUpdateType("all_scope");
        request.setSyncRate(DatasourceTaskServer.ScheduleType.SIMPLE_CRON.name());
        request.setSimpleCronValue(30L);
        request.setSimpleCronType("minute");

        manage.save(request);

        verify(taskMapper).update(any(CoreDatasetSyncTask.class), any(Wrapper.class));
    }
}
