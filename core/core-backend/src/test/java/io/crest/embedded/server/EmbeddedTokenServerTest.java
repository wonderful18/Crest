package io.crest.embedded.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.crest.constant.AuthConstant;
import io.crest.exception.CrestException;
import io.crest.utils.SignedTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddedTokenServerTest {

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void tokenArgsRequireVerifiedEmbeddedToken() {
        EmbeddedTokenServer server = new EmbeddedTokenServer();
        ReflectionTestUtils.setField(server, "tokenSecret", "embedded-token-secret");
        String token = JWT.create()
                .withClaim("args", Map.of("projectId", "P100"))
                .withClaim("tenant", "demo")
                .sign(Algorithm.HMAC256("embedded-token-secret"));
        requestWithEmbeddedToken(token);

        Map<String, Object> args = server.getTokenArgs();

        assertEquals("P100", args.get("projectId"));
        assertEquals("demo", args.get("tenant"));
    }

    @Test
    void tokenArgsRejectForgedEmbeddedToken() {
        EmbeddedTokenServer server = new EmbeddedTokenServer();
        ReflectionTestUtils.setField(server, "tokenSecret", "embedded-token-secret");
        String forged = SignedTokenUtils.sign(Map.of("tenant", "demo"), "wrong-secret", null);
        requestWithEmbeddedToken(forged);

        assertThrows(CrestException.class, server::getTokenArgs);
    }

    private void requestWithEmbeddedToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthConstant.EMBEDDED_TOKEN_KEY, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
