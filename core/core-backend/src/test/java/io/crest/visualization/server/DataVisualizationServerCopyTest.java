package io.crest.visualization.server;

import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.utils.AuthUtils;
import io.crest.utils.IDUtils;
import io.crest.utils.SnowFlake;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import io.crest.visualization.manage.CoreVisualizationManage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataVisualizationServerCopyTest {

    @BeforeEach
    void setUp() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        ReflectionTestUtils.setField(IDUtils.class, "snowFlake", new SnowFlake(1, 1));
    }

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void copyShouldAllowNonCreatorWithManagePermission() {
        DataVisualizationInfoMapper visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        ExtDataVisualizationMapper extDataVisualizationMapper = mock(ExtDataVisualizationMapper.class);
        CoreVisualizationManage coreVisualizationManage = mock(CoreVisualizationManage.class);
        DataVisualizationServer server = new DataVisualizationServer();
        ReflectionTestUtils.setField(server, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(server, "extDataVisualizationMapper", extDataVisualizationMapper);
        ReflectionTestUtils.setField(server, "coreVisualizationManage", coreVisualizationManage);

        DataVisualizationInfo source = new DataVisualizationInfo();
        source.setId(10L);
        source.setName("经营大屏");
        source.setType("dataV");
        source.setNodeType("leaf");
        source.setCreateBy("3");
        source.setOrgId(1L);
        source.setComponentData("[{\"id\":\"100\"}]");
        when(visualizationInfoMapper.selectById(10L)).thenReturn(source);
        when(extDataVisualizationMapper.findViewInfoByCopyId(anyLong())).thenReturn(List.<CoreChartView>of());

        DataVisualizationBaseRequest request = new DataVisualizationBaseRequest();
        request.setId(10L);
        request.setPid(0L);
        request.setName("经营大屏-copy");
        request.setType("dataV");

        assertDoesNotThrow(() -> server.copy(request));

        verify(coreVisualizationManage).requireVisualizationManage(any(DataVisualizationInfo.class));
        verify(extDataVisualizationMapper).viewCopyWithDv(eq(10L), anyLong(), anyLong(), eq("core"));
        verify(extDataVisualizationMapper).viewCopyWithDv(eq(10L), anyLong(), anyLong(), eq("snapshot"));
        verify(coreVisualizationManage).preInnerSave(any(DataVisualizationInfo.class));
    }
}
