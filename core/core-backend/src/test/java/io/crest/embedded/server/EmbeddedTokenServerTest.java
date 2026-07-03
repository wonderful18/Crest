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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertFalse(args.containsKey("uid"));
        assertFalse(args.containsKey("resourceId"));
    }

    @Test
    void tokenArgsRejectForgedEmbeddedToken() {
        EmbeddedTokenServer server = new EmbeddedTokenServer();
        ReflectionTestUtils.setField(server, "tokenSecret", "embedded-token-secret");
        String forged = SignedTokenUtils.sign(Map.of("tenant", "demo"), "wrong-secret", null);
        requestWithEmbeddedToken(forged);

        assertThrows(CrestException.class, server::getTokenArgs);
    }

    @Test
    void tokenArgsKeepCurrentAndLegacyRoutes() throws NoSuchMethodException {
        Method method = EmbeddedTokenServer.class.getDeclaredMethod("getTokenArgs");
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertEquals(2, mapping.value().length);
        assertFalse(Arrays.stream(mapping.value()).noneMatch("/token-args"::equals));
        assertFalse(Arrays.stream(mapping.value()).noneMatch("/getTokenArgs"::equals));
    }

    private void requestWithEmbeddedToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthConstant.EMBEDDED_TOKEN_KEY, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
