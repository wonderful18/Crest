package io.crest.security;

import io.crest.api.font.dto.FontDto;
import io.crest.api.visualization.request.VisualizationSubjectRequest;
import io.crest.api.visualization.request.VisualizationWatermarkRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.font.manage.FontManage;
import io.crest.utils.AuthUtils;
import io.crest.visualization.server.VisualizationSubjectService;
import io.crest.visualization.server.VisualizationWatermarkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlobalConfigurationAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void fontCreateRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        FontManage manage = new FontManage();

        CrestException exception = assertThrows(CrestException.class, () -> manage.create(new FontDto()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void fontUploadRequiresAdminBeforeReadingFile() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        FontManage manage = new FontManage();

        CrestException exception = assertThrows(CrestException.class, () -> manage.upload(null));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void subjectUpdateRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        VisualizationSubjectService service = new VisualizationSubjectService();

        CrestException exception = assertThrows(CrestException.class,
                () -> service.update(new VisualizationSubjectRequest()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void watermarkSaveRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        VisualizationWatermarkService service = new VisualizationWatermarkService();

        CrestException exception = assertThrows(CrestException.class,
                () -> service.saveWatermarkInfo(new VisualizationWatermarkRequest()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }
}
