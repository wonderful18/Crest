package io.crest.security;

import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.log.AuditLogController;
import io.crest.result.ResultCode;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogControllerSecurityTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    @DisplayName("审计日志接口应仅允许管理员或审计角色访问")
    void auditLogApisShouldRequireAuditAccess() throws Exception {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        AuditLogController controller = new AuditLogController();
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        injectPlatformPermissionManage(controller, platformPermissionManage);
        when(platformPermissionManage.isSystemAdmin(2L)).thenReturn(false);
        when(platformPermissionManage.defaultOrgId(2L)).thenReturn(1L);
        when(platformPermissionManage.roleIds(2L, 1L)).thenReturn(List.of());

        CrestException pagerError = assertThrows(CrestException.class,
                () -> controller.pager(1, 15, Map.of()));
        CrestException statisticsError = assertThrows(CrestException.class, controller::statistics);
        CrestException operationTypesError = assertThrows(CrestException.class, controller::operationTypes);

        assertEquals(ResultCode.PERMISSION_NO_ACCESS.code().intValue(), pagerError.getCode());
        assertEquals(ResultCode.PERMISSION_NO_ACCESS.code().intValue(), statisticsError.getCode());
        assertEquals(ResultCode.PERMISSION_NO_ACCESS.code().intValue(), operationTypesError.getCode());
    }

    @Test
    @DisplayName("审计日志分页参数应做边界保护")
    void auditLogPagerShouldClampPageArguments() throws Exception {
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        AuditLogController controller = new AuditLogController();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        injectJdbcTemplate(controller, jdbcTemplate);
        injectPlatformPermissionManage(controller, platformPermissionManage);
        when(platformPermissionManage.isSystemAdmin(1L)).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(java.util.List.of());

        Map<String, Object> result = controller.pager(-10, 1000, Map.of());

        assertEquals(1, result.get("current"));
        assertEquals(200, result.get("size"));
        verify(jdbcTemplate).queryForList(anyString(), aryEq(new Object[]{200, 0}));
    }

    private void injectJdbcTemplate(AuditLogController controller, JdbcTemplate jdbcTemplate) throws Exception {
        Field field = AuditLogController.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(controller, jdbcTemplate);
    }

    private void injectPlatformPermissionManage(
            AuditLogController controller,
            PlatformPermissionManage platformPermissionManage) throws Exception {
        Field field = AuditLogController.class.getDeclaredField("platformPermissionManage");
        field.setAccessible(true);
        field.set(controller, platformPermissionManage);
    }
}
