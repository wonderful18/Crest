package io.crest.dataset.manage;

import io.crest.api.dataset.dto.SqlLogDTO;
import io.crest.dataset.dao.auto.entity.CoreDatasetTable;
import io.crest.dataset.dao.auto.entity.CoreDatasetTableSqlLog;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableSqlLogMapper;
import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetTableSqlLogManageAccessTest {

    @Test
    void saveRejectsUnauthorizedDatasetTableBeforeWritingLog() {
        CoreDatasetTableSqlLogMapper logMapper = mock(CoreDatasetTableSqlLogMapper.class);
        DatasetGroupManage datasetGroupManage = unauthorizedDatasetGroupManage();
        DatasetTableSqlLogManage manage = manage(logMapper, tableMapper(), datasetGroupManage);
        SqlLogDTO dto = new SqlLogDTO();
        dto.setTableId("10");
        dto.setSql("select 1");

        CrestException exception = assertThrows(CrestException.class, () -> manage.save(dto));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(logMapper, never()).insert(any(CoreDatasetTableSqlLog.class));
        verify(logMapper, never()).updateById(any(CoreDatasetTableSqlLog.class));
    }

    @Test
    void listByTableIdRejectsUnauthorizedDatasetTableBeforeQueryingLogs() {
        CoreDatasetTableSqlLogMapper logMapper = mock(CoreDatasetTableSqlLogMapper.class);
        DatasetTableSqlLogManage manage = manage(logMapper, tableMapper(), unauthorizedDatasetGroupManage());
        SqlLogDTO dto = new SqlLogDTO();
        dto.setTableId("10");

        CrestException exception = assertThrows(CrestException.class, () -> manage.listByTableId(dto));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(logMapper, never()).selectList(any());
    }

    @Test
    void tableRemovalRejectsUnauthorizedDatasetTableBeforeDeletingLogs() {
        CoreDatasetTableSqlLogMapper logMapper = mock(CoreDatasetTableSqlLogMapper.class);
        DatasetTableSqlLogManage manage = manage(logMapper, tableMapper(), unauthorizedDatasetGroupManage());

        CrestException exception = assertThrows(CrestException.class, () -> manage.tableRemoval("10"));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(logMapper, never()).delete(any());
    }

    private DatasetTableSqlLogManage manage(CoreDatasetTableSqlLogMapper logMapper,
                                            CoreDatasetTableMapper tableMapper,
                                            DatasetGroupManage datasetGroupManage) {
        DatasetTableSqlLogManage manage = new DatasetTableSqlLogManage();
        ReflectionTestUtils.setField(manage, "coreDatasetTableSqlLogMapper", logMapper);
        ReflectionTestUtils.setField(manage, "coreDatasetTableMapper", tableMapper);
        ReflectionTestUtils.setField(manage, "datasetGroupManage", datasetGroupManage);
        return manage;
    }

    private CoreDatasetTableMapper tableMapper() {
        CoreDatasetTableMapper mapper = mock(CoreDatasetTableMapper.class);
        CoreDatasetTable table = new CoreDatasetTable();
        table.setId(10L);
        table.setDatasetGroupId(20L);
        when(mapper.selectById(10L)).thenReturn(table);
        return mapper;
    }

    private DatasetGroupManage unauthorizedDatasetGroupManage() {
        DatasetGroupManage manage = mock(DatasetGroupManage.class);
        doAnswer(invocation -> {
            CrestException.throwException("当前用户无权访问该资源");
            return null;
        }).when(manage).requireDatasetAccess(20L);
        return manage;
    }
}
