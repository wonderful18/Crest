package io.crest.chart.manage;

import io.crest.auth.bo.TokenUserBO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.chart.dao.ext.mapper.ExtChartViewMapper;
import io.crest.exception.CrestException;
import io.crest.portal.DataPortalPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartViewManegeAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void checkSameDataSetRejectsUnreadableChartBeforeComparing() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreChartViewMapper chartMapper = mock(CoreChartViewMapper.class);
        ChartViewManege manage = manage(chartMapper, mock(ExtChartViewMapper.class), mock(DataVisualizationInfoMapper.class), mock(DataPortalPermissionManage.class));
        CoreChartView chart = new CoreChartView();
        chart.setId(10L);
        chart.setCreateBy("3");
        when(chartMapper.selectById("10")).thenReturn(chart);

        CrestException exception = assertThrows(CrestException.class, () -> manage.checkSameDataSet("10", "11"));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(chartMapper, never()).selectById("11");
    }

    @Test
    void viewOptionRejectsUnreadableSceneBeforeQueryingOptions() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        ExtChartViewMapper extChartViewMapper = mock(ExtChartViewMapper.class);
        DataVisualizationInfoMapper visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        DataPortalPermissionManage dataPortalPermissionManage = mock(DataPortalPermissionManage.class);
        ChartViewManege manage = manage(mock(CoreChartViewMapper.class), extChartViewMapper, visualizationInfoMapper, dataPortalPermissionManage);

        DataVisualizationInfo scene = new DataVisualizationInfo();
        scene.setId(20L);
        scene.setCreateBy("3");
        when(visualizationInfoMapper.selectById(20L)).thenReturn(scene);
        when(dataPortalPermissionManage.canReadPublishedVisualization(anyLong(), isNull())).thenReturn(false);

        CrestException exception = assertThrows(CrestException.class, () -> manage.viewOption(20L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(extChartViewMapper, never()).queryViewOption(20L);
    }

    private ChartViewManege manage(CoreChartViewMapper chartMapper,
                                   ExtChartViewMapper extChartViewMapper,
                                   DataVisualizationInfoMapper visualizationInfoMapper,
                                   DataPortalPermissionManage dataPortalPermissionManage) {
        ChartViewManege manage = new ChartViewManege();
        ReflectionTestUtils.setField(manage, "coreChartViewMapper", chartMapper);
        ReflectionTestUtils.setField(manage, "extChartViewMapper", extChartViewMapper);
        ReflectionTestUtils.setField(manage, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(manage, "dataPortalPermissionManage", dataPortalPermissionManage);
        return manage;
    }
}
