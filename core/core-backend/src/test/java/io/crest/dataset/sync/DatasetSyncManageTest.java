package io.crest.dataset.sync;

import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTask;
import io.crest.dataset.manage.DatasetGroupManage;
import io.crest.exception.CrestException;
import io.crest.utils.CommonThreadPool;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetSyncManageTest {

    @Test
    void executeNowRejectsDisabledSyncModeBeforeSubmittingAsyncTask() throws Exception {
        DatasetSyncManage manage = new DatasetSyncManage();
        DatasetSyncTaskManage taskManage = mock(DatasetSyncTaskManage.class);
        DatasetGroupManage datasetGroupManage = mock(DatasetGroupManage.class);
        CommonThreadPool commonThreadPool = mock(CommonThreadPool.class);
        ReflectionTestUtils.setField(manage, "taskManage", taskManage);
        ReflectionTestUtils.setField(manage, "datasetGroupManage", datasetGroupManage);
        ReflectionTestUtils.setField(manage, "commonThreadPool", commonThreadPool);

        CoreDatasetSyncTask task = new CoreDatasetSyncTask();
        task.setId(9L);
        task.setDatasetGroupId(982004L);
        when(taskManage.selectByDatasetGroupId(982004L)).thenReturn(task);
        DatasetGroupInfoDTO dataset = new DatasetGroupInfoDTO();
        dataset.setId(982004L);
        dataset.setNodeType("dataset");
        dataset.setMode(0);
        when(datasetGroupManage.getForCount(982004L)).thenReturn(dataset);

        CrestException exception = assertThrows(CrestException.class, () -> manage.executeNow(982004L));

        assertEquals("请先开启数据集同步模式", exception.getMsg());
        verify(commonThreadPool, never()).addTask(any(Runnable.class));
    }
}
