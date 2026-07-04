package io.crest.security;

import io.crest.log.CrestAuditAspect;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrestAuditAspectTest {

    @Test
    void auditExpressionResolvesParameterPath() throws Exception {
        assertEquals("9", parse("#p0.id"));
    }

    @Test
    void auditExpressionRejectsExecutableExpression() throws Exception {
        assertNull(parse("#p0.getClass().name"));
    }

    @Test
    void auditExpressionRejectsClassMetadataExpression() throws Exception {
        assertNull(parse("#p0.class.name"));
    }

    private String parse(String expression) throws Exception {
        CrestAuditAspect aspect = new CrestAuditAspect();
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(new Object[]{new AuditRequest(9L)});
        when(signature.getMethod()).thenReturn(TestEndpoint.class.getDeclaredMethod("save", AuditRequest.class));

        Method method = CrestAuditAspect.class.getDeclaredMethod("parseExpression", String.class, ProceedingJoinPoint.class);
        method.setAccessible(true);
        return (String) method.invoke(aspect, expression, point);
    }

    private static class TestEndpoint {
        void save(AuditRequest request) {
            request.getId();
        }
    }

    private static class AuditRequest {
        private final Long id;

        private AuditRequest(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
