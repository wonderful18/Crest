package io.crest.system.manage;

import io.crest.api.system.vo.SsoConfigVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SsoManageSecurityTest {

    @Test
    public void nonceMismatchThrowsException() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", "crest-client",
                "nonce", "different-nonce"
        );
        try {
            invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "expected-nonce"));
            fail("Should throw for nonce mismatch");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("nonce"));
        }
    }

    @Test
    public void nonceMatchPassesValidation() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", "crest-client",
                "nonce", "matching-nonce"
        );
        assertDoesNotThrow(() -> invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "matching-nonce")));
    }

    @Test
    public void missingNonceLogsWarningButDoesNotThrow() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", "crest-client"
        );
        assertDoesNotThrow(() -> invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "sent-nonce")));
    }

    @Test
    public void issuerMismatchThrowsException() throws ReflectiveOperationException {
        SsoConfigVO config = validConfig();
        config.setIssuer("https://expected-idp.example.com");
        Map<String, Object> claims = Map.of(
                "iss", "https://wrong-idp.example.com",
                "aud", "crest-client",
                "nonce", "nonce"
        );
        try {
            invokeValidate(claims, config, newSsoState("/workbranch/index", "nonce"));
            fail("Should throw for issuer mismatch");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("签发方"));
        }
    }

    @Test
    public void audienceMismatchThrowsException() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", "wrong-client",
                "nonce", "nonce"
        );
        try {
            invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "nonce"));
            fail("Should throw for audience mismatch");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("受众"));
        }
    }

    @Test
    public void expiredTokenThrowsException() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", "crest-client",
                "nonce", "nonce",
                "exp", 1L
        );
        try {
            invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "nonce"));
            fail("Should throw for expired token");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("过期"));
        }
    }

    @Test
    public void audienceAsListMatchesClientId() throws ReflectiveOperationException {
        Map<String, Object> claims = Map.of(
                "iss", "https://idp.example.com",
                "aud", List.of("crest-client", "other-client"),
                "nonce", "nonce"
        );
        assertDoesNotThrow(() -> invokeValidate(claims, validConfig(), newSsoState("/workbranch/index", "nonce")));
    }

    private void invokeValidate(Map<String, Object> claims, SsoConfigVO config, Object ssoState) throws ReflectiveOperationException {
        SsoManage manage = new SsoManage();
        Class<?> ssoStateClass = findSsoStateClass();
        Method method = SsoManage.class.getDeclaredMethod("validateIdTokenClaims", Map.class, SsoConfigVO.class, ssoStateClass);
        method.setAccessible(true);
        try {
            method.invoke(manage, claims, config, ssoState);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object newSsoState(String redirect, String nonce) throws ReflectiveOperationException {
        Class<?> ssoStateClass = findSsoStateClass();
        Constructor<?> constructor = ssoStateClass.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(redirect, nonce);
    }

    private Class<?> findSsoStateClass() {
        for (Class<?> clazz : SsoManage.class.getDeclaredClasses()) {
            if ("SsoState".equals(clazz.getSimpleName())) {
                return clazz;
            }
        }
        throw new IllegalStateException("SsoState class not found");
    }

    private SsoConfigVO validConfig() {
        SsoConfigVO config = new SsoConfigVO();
        config.setClientId("crest-client");
        config.setIssuer("https://idp.example.com");
        return config;
    }
}
