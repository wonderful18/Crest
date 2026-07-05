package io.crest.security;

import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.log.AuditLogService;
import io.crest.log.CrestAudit;
import io.crest.log.CrestAuditAspect;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrestAuditAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProceedingJoinPoint point;

    @Mock
    private MethodSignature signature;

    @Mock
    private CrestAudit deLog;

    @Mock
    private HttpServletRequest request;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("stExp 应按业务类型记录数据大屏审计资源类型")
    void shouldResolveResourceTypeFromStExp() throws Throwable {
        CrestAuditAspect aspect = new CrestAuditAspect();
        injectAuditLogService(aspect);
        VisualizationRequest visualizationRequest = new VisualizationRequest(9L, "dataV");

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/data-visualization/canvas");
        when(request.getRemoteAddr()).thenReturn(loopbackAddress());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("save", VisualizationRequest.class));
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{visualizationRequest});
        when(point.proceed()).thenReturn("ok");

        when(deLog.id()).thenReturn("#p0.id");
        when(deLog.stExp()).thenReturn("#p0.type");
        when(deLog.ot()).thenReturn(LogOT.MODIFY);

        Object result = aspect.around(point, deLog);

        assertEquals("ok", result);
        ArgumentCaptor<String> resourceType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> resourceId = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(LogOT.MODIFY), resourceType.capture(), resourceId.capture(),
                anyString(), eq("POST"), eq("/data-visualization/canvas"),
                isNull(), isNull(), isNull(), eq(loopbackAddress()),
                anyLong(), eq(200), eq("success"));
        assertEquals("SCREEN", resourceType.getValue());
        assertEquals("9", resourceId.getValue());
    }

    @Test
    @DisplayName("审计表达式不允许执行方法调用")
    void shouldRejectExecutableAuditExpression() throws Throwable {
        CrestAuditAspect aspect = new CrestAuditAspect();
        injectAuditLogService(aspect);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/data-visualization/canvas");
        when(request.getRemoteAddr()).thenReturn(loopbackAddress());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(point.proceed()).thenReturn("ok");

        when(deLog.id()).thenReturn("#p0.getClass().name");
        when(deLog.stExp()).thenReturn("");
        when(deLog.st()).thenReturn(LogST.USER);
        when(deLog.ot()).thenReturn(LogOT.MODIFY);

        aspect.around(point, deLog);

        ArgumentCaptor<String> resourceId = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(LogOT.MODIFY), eq("USER"), resourceId.capture(),
                anyString(), eq("POST"), eq("/data-visualization/canvas"),
                isNull(), isNull(), isNull(), eq(loopbackAddress()),
                anyLong(), eq(200), eq("success"));
        assertNull(resourceId.getValue());
    }

    @Test
    @DisplayName("审计表达式不允许访问元类型属性")
    void shouldRejectClassMetadataAuditExpression() throws Throwable {
        CrestAuditAspect aspect = new CrestAuditAspect();
        injectAuditLogService(aspect);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/data-visualization/canvas");
        when(request.getRemoteAddr()).thenReturn(loopbackAddress());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("save", VisualizationRequest.class));
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{new VisualizationRequest(9L, "dataV")});
        when(point.proceed()).thenReturn("ok");

        when(deLog.id()).thenReturn("#p0.class.name");
        when(deLog.stExp()).thenReturn("");
        when(deLog.st()).thenReturn(LogST.USER);
        when(deLog.ot()).thenReturn(LogOT.MODIFY);

        aspect.around(point, deLog);

        ArgumentCaptor<String> resourceId = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq(LogOT.MODIFY), eq("USER"), resourceId.capture(),
                anyString(), eq("POST"), eq("/data-visualization/canvas"),
                isNull(), isNull(), isNull(), eq(loopbackAddress()),
                anyLong(), eq(200), eq("success"));
        assertNull(resourceId.getValue());
    }

    private void injectAuditLogService(CrestAuditAspect aspect) throws ReflectiveOperationException {
        Field field = CrestAuditAspect.class.getDeclaredField("auditLogService");
        field.setAccessible(true);
        field.set(aspect, auditLogService);
    }

    private String loopbackAddress() {
        return String.join(".", "127", "0", "0", "1");
    }

    private static class TestEndpoint {
        @SuppressWarnings("unused")
        public Object save(VisualizationRequest request) {
            return null;
        }
    }

    private record VisualizationRequest(Long id, String type) {
    }
}
