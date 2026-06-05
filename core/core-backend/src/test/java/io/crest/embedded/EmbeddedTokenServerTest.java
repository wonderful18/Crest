package io.crest.embedded;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.crest.constant.AuthConstant;
import io.crest.embedded.server.EmbeddedTokenServer;
import io.crest.utils.ServletUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedTokenServerTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getTokenShouldFallbackToCurrentEmbeddedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthConstant.EMBEDDED_TOKEN_KEY, "embedded-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("embedded-token", ServletUtils.getToken());
        assertEquals("embedded-token", ServletUtils.getEmbeddedToken());
    }

    @Test
    void getTokenShouldFallbackToLegacyEmbeddedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthConstant.LEGACY_EMBEDDED_TOKEN_KEY, "legacy-embedded-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("legacy-embedded-token", ServletUtils.getToken());
        assertEquals("legacy-embedded-token", ServletUtils.getEmbeddedToken());
    }

    @Test
    void getTokenArgsShouldExposeCustomClaimsOnly() {
        String token = JWT.create()
                .withClaim("uid", 1L)
                .withClaim("oid", 2L)
                .withClaim("resourceId", 3L)
                .withClaim("city", "shanghai")
                .withClaim("args", Map.of("region", "east", "level", 2))
                .sign(Algorithm.HMAC256("secret"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthConstant.EMBEDDED_TOKEN_KEY, token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Map<String, Object> args = new EmbeddedTokenServer().getTokenArgs();

        assertEquals("shanghai", args.get("city"));
        assertEquals("east", args.get("region"));
        assertEquals(2, args.get("level"));
        assertFalse(args.containsKey("uid"));
        assertFalse(args.containsKey("oid"));
        assertFalse(args.containsKey("resourceId"));
    }

    @Test
    void getTokenArgsShouldExposeCurrentAndLegacyPaths() throws NoSuchMethodException {
        Method method = EmbeddedTokenServer.class.getDeclaredMethod("getTokenArgs");
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/token-args"));
        assertTrue(Arrays.asList(mapping.value()).contains("/getTokenArgs"));
    }
}
