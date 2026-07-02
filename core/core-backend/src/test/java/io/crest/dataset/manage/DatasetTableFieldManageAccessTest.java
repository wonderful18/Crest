package io.crest.dataset.manage;

import io.crest.auth.bo.TokenUserBO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.dataset.dao.auto.entity.CoreDatasetGroup;
import io.crest.dataset.dao.auto.entity.CoreDatasetTableField;
import io.crest.dataset.dao.auto.mapper.CoreDatasetGroupMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableFieldMapper;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DatasetTableFieldManageAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void chartFieldSaveRejectsChartNotManagedByCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreDatasetTableFieldMapper fieldMapper = mock(CoreDatasetTableFieldMapper.class);
        DatasetTableFieldManage manage = manage(fieldMapper, chartMapper("3"), mock(CoreDatasetGroupMapper.class));

        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(100L);
        field.setChartId(10L);
        field.setName("计算字段");

        CrestException exception = assertThrows(CrestException.class, () -> manage.chartFieldSave(field));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(fieldMapper, never()).selectList(null);
    }

    @Test
    void deleteByIdRejectsDatasetFieldNotManagedByCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreDatasetTableFieldMapper fieldMapper = mock(CoreDatasetTableFieldMapper.class);
        CoreDatasetGroupMapper datasetGroupMapper = mock(CoreDatasetGroupMapper.class);
        DatasetTableFieldManage manage = manage(fieldMapper, mock(CoreChartViewMapper.class), datasetGroupMapper);

        CoreDatasetTableField field = new CoreDatasetTableField();
        field.setId(100L);
        field.setDatasetGroupId(20L);
        when(fieldMapper.selectById(100L)).thenReturn(field);
        CoreDatasetGroup dataset = new CoreDatasetGroup();
        dataset.setId(20L);
        dataset.setCreateBy("3");
        when(datasetGroupMapper.selectById(20L)).thenReturn(dataset);

        CrestException exception = assertThrows(CrestException.class, () -> manage.deleteById(100L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(fieldMapper, never()).deleteById(100L);
    }

    @Test
    void selectByIdWithAccessRejectsDatasetFieldNotManagedByCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreDatasetTableFieldMapper fieldMapper = mock(CoreDatasetTableFieldMapper.class);
        CoreDatasetGroupMapper datasetGroupMapper = mock(CoreDatasetGroupMapper.class);
        DatasetTableFieldManage manage = manage(fieldMapper, mock(CoreChartViewMapper.class), datasetGroupMapper);

        CoreDatasetTableField field = new CoreDatasetTableField();
        field.setId(100L);
        field.setDatasetGroupId(20L);
        when(fieldMapper.selectById(100L)).thenReturn(field);
        CoreDatasetGroup dataset = new CoreDatasetGroup();
        dataset.setId(20L);
        dataset.setCreateBy("3");
        when(datasetGroupMapper.selectById(20L)).thenReturn(dataset);

        CrestException exception = assertThrows(CrestException.class, () -> manage.selectByIdWithAccess(100L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void selectByDatasetGroupIdWithAccessRejectsBeforeQueryingFields() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreDatasetTableFieldMapper fieldMapper = mock(CoreDatasetTableFieldMapper.class);
        CoreDatasetGroupMapper datasetGroupMapper = mock(CoreDatasetGroupMapper.class);
        DatasetTableFieldManage manage = manage(fieldMapper, mock(CoreChartViewMapper.class), datasetGroupMapper);

        CoreDatasetGroup dataset = new CoreDatasetGroup();
        dataset.setId(20L);
        dataset.setCreateBy("3");
        when(datasetGroupMapper.selectById(20L)).thenReturn(dataset);

        CrestException exception = assertThrows(CrestException.class, () -> manage.selectByDatasetGroupIdWithAccess(20L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(fieldMapper, never()).selectList(any());
    }

    @Test
    void deleteChartFieldsRejectsChartNotManagedByCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreDatasetTableFieldMapper fieldMapper = mock(CoreDatasetTableFieldMapper.class);
        DatasetTableFieldManage manage = manage(fieldMapper, chartMapper("3"), mock(CoreDatasetGroupMapper.class));

        CrestException exception = assertThrows(CrestException.class, () -> manage.deleteChartFields(10L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(fieldMapper, never()).delete(any());
    }

    private DatasetTableFieldManage manage(CoreDatasetTableFieldMapper fieldMapper,
                                           CoreChartViewMapper chartMapper,
                                           CoreDatasetGroupMapper datasetGroupMapper) {
        DatasetTableFieldManage manage = new DatasetTableFieldManage();
        DatasetGroupManage datasetGroupManage = new DatasetGroupManage();
        ReflectionTestUtils.setField(datasetGroupManage, "coreDatasetGroupMapper", datasetGroupMapper);
        ReflectionTestUtils.setField(manage, "coreDatasetTableFieldMapper", fieldMapper);
        ReflectionTestUtils.setField(manage, "coreChartViewMapper", chartMapper);
        ReflectionTestUtils.setField(manage, "datasetGroupManage", datasetGroupManage);
        ReflectionTestUtils.setField(manage, "visualizationInfoMapper", mock(DataVisualizationInfoMapper.class));
        return manage;
    }

    private CoreChartViewMapper chartMapper(String createBy) {
        CoreChartViewMapper mapper = mock(CoreChartViewMapper.class);
        CoreChartView chart = new CoreChartView();
        chart.setId(10L);
        chart.setCreateBy(createBy);
        when(mapper.selectById(10L)).thenReturn(chart);
        return mapper;
    }
}
