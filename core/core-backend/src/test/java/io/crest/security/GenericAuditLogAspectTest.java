package io.crest.security;

import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.log.AuditLogService;
import io.crest.log.CrestAudit;
import io.crest.log.GenericAuditLogAspect;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericAuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProceedingJoinPoint point;

    @Mock
    private MethodSignature signature;

    @Mock
    private HttpServletRequest request;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("未标注 CrestAudit 的写接口应自动记录审计日志")
    void shouldAuditUnannotatedWriteApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/role/create");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("create"));
        when(signature.getName()).thenReturn("create");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.CREATE), eq("ROLE"), isNull(), contains("角色"),
                eq("POST"), eq("/role/create"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("未标注 CrestAudit 的导出下载类 GET 接口也应记录审计日志")
    void shouldAuditUnannotatedExportOrDownloadApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("GET", "/export-center/download/task-1");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("download"));
        when(signature.getName()).thenReturn("download");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn(null);

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.DOWNLOAD), eq("DATA"), isNull(), contains("下载"),
                eq("GET"), eq("/export-center/download/task-1"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("导出中心列表查询应按查看记录，避免被 export 关键词误判为导出")
    void shouldClassifyExportCenterTaskQueryAsRead() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/export-center/export-tasks/records");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("exportTasks"));
        when(signature.getName()).thenReturn("exportTasks");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.READ), eq("DATA"), isNull(), contains("查看"),
                eq("POST"), eq("/export-center/export-tasks/records"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("普通只读 GET 接口不应被兜底审计放大噪声")
    void shouldSkipPlainReadApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("GET", "/user/info");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("info"));
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("历史 GET 删除清理接口应纳入兜底审计")
    void shouldAuditMutatingGetApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("GET", "/sysVariable/delete/1");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("delete"));
        when(signature.getName()).thenReturn("delete");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn(null);

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.DELETE), eq("DATA"), isNull(), contains("删除"),
                eq("GET"), eq("/sysVariable/delete/1"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("未标注 CrestAudit 的分享创建接口应按分享链接归类")
    void shouldClassifyShareLinkApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/share/create");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("create"));
        when(signature.getName()).thenReturn("create");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.CREATE), eq("LINK"), isNull(), contains("分享链接"),
                eq("POST"), eq("/share/create"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("未标注 CrestAudit 的驱动上传接口应按驱动归类")
    void shouldClassifyDatasourceDriverApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/datasource-driver/upload");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("upload"));
        when(signature.getName()).thenReturn("upload");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.UPLOADFILE), eq("DRIVER"), isNull(), contains("驱动"),
                eq("POST"), eq("/datasource-driver/upload"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("当前权限查询接口应按查看记录，而不是旧 save/record 路由")
    void shouldClassifyCurrentPermissionReadApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/auth/business-permissions");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("permission"));
        when(signature.getName()).thenReturn("permission");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.READ), eq("DATA"), isNull(), eq("查看业务资源权限"),
                eq("POST"), eq("/auth/business-permissions"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("当前权限保存接口应按授权记录")
    void shouldClassifyCurrentPermissionWriteApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("PUT", "/auth/menu-permissions");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("permission"));
        when(signature.getName()).thenReturn("permission");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.AUTHORIZE), eq("MENU"), isNull(), eq("配置菜单权限"),
                eq("PUT"), eq("/auth/menu-permissions"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("当前 dataset-tree 路由应归为数据集资源")
    void shouldClassifyCurrentDatasetTreeApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/dataset-tree/tree");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("tree"));
        when(signature.getName()).thenReturn("tree");
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verify(auditLogService).log(eq(LogOT.READ), eq("DATASET"), isNull(), contains("数据集"),
                eq("POST"), eq("/dataset-tree/tree"), isNull(), isNull(), isNull(), eq("127.0.0.1"),
                anyLong(), eq(200), eq("success"));
    }

    @Test
    @DisplayName("已标注 CrestAudit 的接口不应再被兜底审计重复记录")
    void shouldSkipAnnotatedApi() throws Throwable {
        GenericAuditLogAspect aspect = new GenericAuditLogAspect();
        injectAuditLogService(aspect);
        bindRequest("POST", "/sso/config");
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("annotatedSave"));
        when(point.getSignature()).thenReturn(signature);
        when(point.proceed()).thenReturn("ok");

        aspect.around(point);

        verifyNoInteractions(auditLogService);
    }

    private void bindRequest(String method, String uri) {
        lenient().when(request.getMethod()).thenReturn(method);
        lenient().when(request.getRequestURI()).thenReturn(uri);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void injectAuditLogService(GenericAuditLogAspect aspect) throws ReflectiveOperationException {
        Field field = GenericAuditLogAspect.class.getDeclaredField("auditLogService");
        field.setAccessible(true);
        field.set(aspect, auditLogService);
    }

    private static class TestEndpoint {
        @PostMapping("/create")
        public Object create() {
            return null;
        }

        @GetMapping("/download")
        public Object download() {
            return null;
        }

        @GetMapping("/info")
        public Object info() {
            return null;
        }

        @GetMapping("/delete")
        public Object delete() {
            return null;
        }

        @PostMapping("/upload")
        public Object upload() {
            return null;
        }

        @PostMapping("/permission")
        public Object permission() {
            return null;
        }

        @PostMapping("/tree")
        public Object tree() {
            return null;
        }

        @PostMapping("/exportTasks")
        public Object exportTasks() {
            return null;
        }

        @CrestAudit(ot = LogOT.MODIFY, st = LogST.DATA)
        @PostMapping("/sso/config")
        public Object annotatedSave() {
            return null;
        }
    }
}
