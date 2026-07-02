package io.crest.share.manage;

import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.share.dao.auto.entity.CoreShare;
import io.crest.share.dao.auto.mapper.CoreShareMapper;
import io.crest.share.dao.ext.mapper.ShareExtMapper;
import io.crest.utils.AuthUtils;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShareManageSecurityTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void switcherRejectsResourceNotOwnedByCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CoreShareMapper coreShareMapper = mock(CoreShareMapper.class);
        ShareExtMapper shareExtMapper = mock(ShareExtMapper.class);
        ShareTicketManage shareTicketManage = mock(ShareTicketManage.class);
        DataVisualizationInfoMapper dataVisualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        ShareManage shareManage = new ShareManage();
        ReflectionTestUtils.setField(shareManage, "coreShareMapper", coreShareMapper);
        ReflectionTestUtils.setField(shareManage, "shareExtMapper", shareExtMapper);
        ReflectionTestUtils.setField(shareManage, "shareTicketManage", shareTicketManage);
        ReflectionTestUtils.setField(shareManage, "dataVisualizationInfoMapper", dataVisualizationInfoMapper);

        DataVisualizationInfo info = new DataVisualizationInfo();
        info.setId(20L);
        info.setCreateBy("3");
        when(coreShareMapper.selectOne(any())).thenReturn(null);
        when(dataVisualizationInfoMapper.selectById(20L)).thenReturn(info);

        CrestException exception = assertThrows(CrestException.class, () -> shareManage.switcher(20L));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verify(coreShareMapper, never()).insert(any(CoreShare.class));
        verify(shareExtMapper, never()).visualizationType(any());
    }
}
