package io.crest.security;

import io.crest.share.interceptor.CrestLinkAspect;
import io.crest.share.manage.ShareLinkAccessManage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class CrestLinkAspectSecurityTest {

    private final CrestLinkAspect aspect = new CrestLinkAspect(mock(ShareLinkAccessManage.class));

    @Test
    void shouldResolveParameterPath() {
        assertEquals(12L, aspect.getExpression(new Object[]{new LinkRequest(12L)}, "#p0.id"));
    }

    @Test
    void shouldResolveLiteralResourceId() {
        assertEquals(12L, aspect.getExpression(new Object[0], "12"));
    }

    @Test
    void shouldRejectExecutableExpression() {
        assertNull(aspect.getExpression(new Object[]{new LinkRequest(12L)}, "#p0.getClass().name"));
    }

    @Test
    void shouldRejectClassMetadataExpression() {
        assertNull(aspect.getExpression(new Object[]{new LinkRequest(12L)}, "#p0.class.name"));
    }

    @Test
    void shouldRejectMalformedParameterIndex() {
        assertNull(aspect.getExpression(new Object[]{new LinkRequest(12L)}, "#p999999999999999999999.id"));
    }

    private static class LinkRequest {
        private final Long id;

        private LinkRequest(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}
