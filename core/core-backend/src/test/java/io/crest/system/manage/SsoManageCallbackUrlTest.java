package io.crest.system.manage;

import io.crest.api.system.vo.SsoConfigVO;
import io.crest.auth.vo.TokenVO;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.utils.SignedTokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SsoManageCallbackUrlTest {

    @Test
    void callbackUrlUsesValidatedForwardedHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sso/login");
        request.setContextPath("/crest");
        request.setScheme("http");
        request.setServerName("internal.local");
        request.setServerPort(8100);
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "portal.example.com");
        request.addHeader("X-Forwarded-Port", "443");

        String callback = invokeCallbackUrl(request);

        assertEquals("https://portal.example.com/crest/api/v1/sso/callback", callback);
    }

    @Test
    void callbackUrlRejectsForwardedHostWithPath() {
        MockHttpServletRequest request = forwardedRequest("https", "portal.example.com/callback", "443");

        assertThrows(CrestException.class, () -> invokeCallbackUrl(request));
    }

    @Test
    void callbackUrlRejectsForwardedHostWithUserInfo() {
        MockHttpServletRequest request = forwardedRequest("https", "user@portal.example.com", "443");

        assertThrows(CrestException.class, () -> invokeCallbackUrl(request));
    }

    @Test
    void callbackUrlRejectsInvalidForwardedPort() {
        MockHttpServletRequest request = forwardedRequest("https", "portal.example.com", "70000");

        assertThrows(CrestException.class, () -> invokeCallbackUrl(request));
    }

    @Test
    void ssoRedirectRejectsExternalAndMalformedTargets() throws Exception {
        assertEquals("/workbranch/index", invokeNormalizeRedirect("https://example.com"));
        assertEquals("/workbranch/index", invokeNormalizeRedirect("//example.com/path"));
        assertEquals("/workbranch/index", invokeNormalizeRedirect("/\\example.com"));
        assertEquals("/workbranch/index", invokeNormalizeRedirect("javascript:alert(1)"));
        assertEquals("/workbranch/index", invokeNormalizeRedirect("/workbranch\u0000/index"));
        assertEquals("/workbranch/index", invokeNormalizeRedirect("workbranch/index"));
    }

    @Test
    void ssoTokenUsesAccessTokenExpiration() throws Exception {
        SsoManage manage = new SsoManage();
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        when(platformPermissionManage.defaultOrgId(7L)).thenReturn(1L);
        ReflectionTestUtils.setField(manage, "platformPermissionManage", platformPermissionManage);
        CrestUser user = new CrestUser();
        user.setId(7L);
        user.setPasswordHash("sso-token-secret");

        TokenVO token = invokeGenerate(manage, user);

        assertEquals(24 * 60 * 60 * 1000L, token.getExp());
        assertNotNull(SignedTokenUtils.verify(token.getToken(), "sso-token-secret").getExpiresAt());
        assertTrue(SignedTokenUtils.verify(token.getToken(), "sso-token-secret").getExpiresAt().getTime() > System.currentTimeMillis());
    }

    private MockHttpServletRequest forwardedRequest(String proto, String host, String port) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sso/login");
        request.setScheme("http");
        request.setServerName("internal.local");
        request.setServerPort(8100);
        request.addHeader("X-Forwarded-Proto", proto);
        request.addHeader("X-Forwarded-Host", host);
        request.addHeader("X-Forwarded-Port", port);
        return request;
    }

    private String invokeCallbackUrl(MockHttpServletRequest request) throws Exception {
        SsoManage manage = new SsoManage();
        Method callbackUrlMethod = SsoManage.class.getDeclaredMethod("callbackUrl", jakarta.servlet.http.HttpServletRequest.class, SsoConfigVO.class);
        callbackUrlMethod.setAccessible(true);
        try {
            return (String) callbackUrlMethod.invoke(manage, request, new SsoConfigVO());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private String invokeNormalizeRedirect(String redirect) throws Exception {
        SsoManage manage = new SsoManage();
        Method normalizeRedirectMethod = SsoManage.class.getDeclaredMethod("normalizeRedirect", String.class);
        normalizeRedirectMethod.setAccessible(true);
        return (String) normalizeRedirectMethod.invoke(manage, redirect);
    }

    private TokenVO invokeGenerate(SsoManage manage, CrestUser user) throws Exception {
        Method generateMethod = SsoManage.class.getDeclaredMethod("generate", CrestUser.class);
        generateMethod.setAccessible(true);
        return (TokenVO) generateMethod.invoke(manage, user);
    }
}
