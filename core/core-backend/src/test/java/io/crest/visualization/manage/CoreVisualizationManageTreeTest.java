package io.crest.visualization.manage;

import io.crest.model.BusiNodeRequest;
import io.crest.model.BusiNodeVO;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.visualization.dao.ext.mapper.CoreVisualiationExtMapper;
import io.crest.visualization.dao.ext.po.VisualizationNodePO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreVisualizationManageTreeTest {

    @Test
    void treeShouldExposeReadOnlyVisualizationAsReadWeight() {
        CoreVisualiationExtMapper extMapper = mock(CoreVisualiationExtMapper.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        CoreVisualizationManage manage = new CoreVisualizationManage();
        ReflectionTestUtils.setField(manage, "extMapper", extMapper);
        ReflectionTestUtils.setField(manage, "platformPermissionManage", platformPermissionManage);

        VisualizationNodePO node = new VisualizationNodePO();
        node.setId(20L);
        node.setName("他人经营大屏");
        node.setPid(0L);
        node.setNodeType("leaf");
        node.setType("dataV");
        node.setCreateBy("3");
        node.setOrgId(1L);
        when(platformPermissionManage.resourceScopeSql(any(), any(), any(), any())).thenReturn(null);
        when(platformPermissionManage.resourceWeight("screen", "20", "3", 1L)).thenReturn(1);
        when(extMapper.queryNodes(any())).thenReturn(List.of(node));

        BusiNodeRequest request = new BusiNodeRequest();
        request.setBusiFlag("dataV");
        List<BusiNodeVO> tree = manage.tree(request);

        assertEquals(1, tree.getFirst().getChildren().getFirst().getWeight());
    }
}
