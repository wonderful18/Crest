package io.crest.visualization.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.crest.api.visualization.request.VisualizationStoreRequest;
import io.crest.api.visualization.request.VisualizationWorkbranchQueryRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.entity.CoreStore;
import io.crest.visualization.dao.auto.mapper.CoreStoreMapper;
import io.crest.visualization.dao.ext.mapper.CoreStoreExtMapper;
import io.crest.visualization.dao.ext.po.StorePO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisualizationStoreManageAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void executeRejectsUnreadableResourceBeforeCreatingFavorite() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreStoreMapper storeMapper = mock(CoreStoreMapper.class);
        CoreStoreExtMapper storeExtMapper = mock(CoreStoreExtMapper.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        VisualizationStoreManage manage = manage(storeMapper, storeExtMapper, platformPermissionManage);
        when(storeMapper.exists(any())).thenReturn(false);
        when(platformPermissionManage.resourceScopeSql(eq("panel"), anyString(), anyString(), anyString()))
                .thenReturn("1 = 0");
        when(storeExtMapper.countReadable(any())).thenReturn(0L);

        CrestException exception = assertThrows(CrestException.class,
                () -> manage.execute(new VisualizationStoreRequest(20L, "panel")));

        assertEquals("资源不存在或无访问权限", exception.getMsg());
        ArgumentCaptor<QueryWrapper<Object>> captor = forClass(QueryWrapper.class);
        verify(storeExtMapper).countReadable(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("v.type"));
        verify(storeMapper, never()).insert(any(CoreStore.class));
        verify(storeMapper, never()).delete(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void queryStorePageAddsVisualizationResourceScope() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreStoreMapper storeMapper = mock(CoreStoreMapper.class);
        CoreStoreExtMapper storeExtMapper = mock(CoreStoreExtMapper.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        VisualizationStoreManage manage = manage(storeMapper, storeExtMapper, platformPermissionManage);
        when(platformPermissionManage.resourceScopeSql(eq("screen"), anyString(), anyString(), anyString()))
                .thenReturn("screen_scope");
        when(platformPermissionManage.resourceScopeSql(eq("panel"), anyString(), anyString(), anyString()))
                .thenReturn("panel_scope");
        when(storeExtMapper.query(any(), any())).thenReturn(new Page<StorePO>());

        VisualizationWorkbranchQueryRequest request = new VisualizationWorkbranchQueryRequest();
        request.setType("panel");
        manage.queryStorePage(1, 20, request);

        ArgumentCaptor<QueryWrapper<Object>> captor = forClass(QueryWrapper.class);
        verify(storeExtMapper).query(any(IPage.class), captor.capture());
        String segment = captor.getValue().getCustomSqlSegment();
        assertTrue(segment.contains("screen_scope"));
        assertTrue(segment.contains("panel_scope"));
    }

    @Test
    void queryStorePageAcceptsNullRequest() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreStoreMapper storeMapper = mock(CoreStoreMapper.class);
        CoreStoreExtMapper storeExtMapper = mock(CoreStoreExtMapper.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        VisualizationStoreManage manage = manage(storeMapper, storeExtMapper, platformPermissionManage);
        when(storeExtMapper.query(any(), any())).thenReturn(new Page<StorePO>());

        manage.queryStorePage(1, 20, null);

        verify(storeExtMapper).query(any(), any());
    }

    private VisualizationStoreManage manage(CoreStoreMapper storeMapper,
                                            CoreStoreExtMapper storeExtMapper,
                                            PlatformPermissionManage platformPermissionManage) {
        VisualizationStoreManage manage = new VisualizationStoreManage();
        ReflectionTestUtils.setField(manage, "coreStoreMapper", storeMapper);
        ReflectionTestUtils.setField(manage, "coreStoreExtMapper", storeExtMapper);
        ReflectionTestUtils.setField(manage, "platformPermissionManage", platformPermissionManage);
        return manage;
    }
}
