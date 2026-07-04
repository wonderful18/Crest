package io.crest.security;

import io.crest.auth.bo.TokenUserBO;
import io.crest.utils.AuthUtils;
import io.crest.websocket.handler.PrincipalHandshakeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PrincipalHandshakeHandlerTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    @DisplayName("WebSocket 握手应使用当前登录用户而不是请求参数 userId")
    void shouldUseAuthenticatedUserInsteadOfRequestParameter() {
        AuthUtils.setUser(new TokenUserBO(42L, 1L));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", "7");

        Principal principal = new TestPrincipalHandshakeHandler().determine(
                new ServletServerHttpRequest(request),
                mock(WebSocketHandler.class)
        );

        assertEquals("42", principal.getName());
    }

    private static class TestPrincipalHandshakeHandler extends PrincipalHandshakeHandler {
        Principal determine(ServletServerHttpRequest request, WebSocketHandler handler) {
            return super.determineUser(request, handler, new HashMap<>());
        }
    }
}
