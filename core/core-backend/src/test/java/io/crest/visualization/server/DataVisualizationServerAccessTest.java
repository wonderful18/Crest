package io.crest.visualization.server;

import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.portal.DataPortalPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import io.crest.visualization.dao.auto.mapper.SnapshotDataVisualizationInfoMapper;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import io.crest.visualization.manage.CoreVisualizationManage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataVisualizationServerAccessTest {

    @BeforeEach
    void setUp() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
    }

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void detailListRejectsUnreadableVisualizationBeforeQueryingDetails() {
        DataVisualizationInfoMapper visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        ExtDataVisualizationMapper extDataVisualizationMapper = mock(ExtDataVisualizationMapper.class);
        DataPortalPermissionManage dataPortalPermissionManage = mock(DataPortalPermissionManage.class);
        DataVisualizationServer server = new DataVisualizationServer();
        ReflectionTestUtils.setField(server, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(server, "extDataVisualizationMapper", extDataVisualizationMapper);
        ReflectionTestUtils.setField(server, "dataPortalPermissionManage", dataPortalPermissionManage);

        DataVisualizationInfo info = new DataVisualizationInfo();
        info.setId(10L);
        info.setType("dataV");
        info.setCreateBy("3");
        when(visualizationInfoMapper.selectById(10L)).thenReturn(info);
        when(dataPortalPermissionManage.canReadPublishedVisualization(10L, "dataV")).thenReturn(false);

        assertThrows(CrestException.class, () -> server.detailList(10L));

        verify(extDataVisualizationMapper, never()).getVisualizationViewDetails(anyLong());
    }

    @Test
    void detailListAllowsPublishedVisualizationReadScope() {
        DataVisualizationInfoMapper visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        ExtDataVisualizationMapper extDataVisualizationMapper = mock(ExtDataVisualizationMapper.class);
        SnapshotDataVisualizationInfoMapper snapshotMapper = mock(SnapshotDataVisualizationInfoMapper.class);
        DataPortalPermissionManage dataPortalPermissionManage = mock(DataPortalPermissionManage.class);
        DataVisualizationServer server = new DataVisualizationServer();
        ReflectionTestUtils.setField(server, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(server, "extDataVisualizationMapper", extDataVisualizationMapper);
        ReflectionTestUtils.setField(server, "snapshotMapper", snapshotMapper);
        ReflectionTestUtils.setField(server, "dataPortalPermissionManage", dataPortalPermissionManage);

        DataVisualizationInfo info = new DataVisualizationInfo();
        info.setId(10L);
        info.setType("dataV");
        info.setCreateBy("3");
        when(visualizationInfoMapper.selectById(10L)).thenReturn(info);
        when(dataPortalPermissionManage.canReadPublishedVisualization(10L, "dataV")).thenReturn(true);
        when(extDataVisualizationMapper.getVisualizationViewDetails(10L)).thenReturn(List.of());

        assertTrue(server.detailList(10L).isEmpty());

        verify(extDataVisualizationMapper).getVisualizationViewDetails(10L);
    }

    @Test
    void updateCheckVersionRequiresManagePermissionBeforeUpdating() {
        DataVisualizationInfoMapper visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        CoreVisualizationManage coreVisualizationManage = mock(CoreVisualizationManage.class);
        DataVisualizationServer server = new DataVisualizationServer();
        ReflectionTestUtils.setField(server, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(server, "coreVisualizationManage", coreVisualizationManage);
        ReflectionTestUtils.setField(server, "crestVersion", "2.1.0");

        DataVisualizationInfo current = new DataVisualizationInfo();
        current.setId(10L);
        current.setType("dataV");
        current.setCreateBy("2");
        when(visualizationInfoMapper.selectById(10L)).thenReturn(current);

        server.updateCheckVersion(10L);

        verify(coreVisualizationManage).requireVisualizationManage(current);
        ArgumentCaptor<DataVisualizationInfo> captor = ArgumentCaptor.forClass(DataVisualizationInfo.class);
        verify(visualizationInfoMapper).updateById(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertEquals("2.1.0", captor.getValue().getCheckVersion());
    }
}
