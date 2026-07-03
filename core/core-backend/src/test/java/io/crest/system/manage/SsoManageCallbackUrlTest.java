package io.crest.system.manage;

import io.crest.api.system.vo.SsoConfigVO;
import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
