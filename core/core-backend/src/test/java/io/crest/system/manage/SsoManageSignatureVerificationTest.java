package io.crest.system.manage;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.crest.api.system.vo.SsoConfigVO;
import io.crest.utils.HttpClientConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

/**
 * 验证 SSO id_token JWKS 签名验证：
 * 1. 生成 RSA 密钥对
 * 2. 用私钥签名 JWT
 * 3. 构造 JWKS 响应（mock HttpClientUtil.get 返回）
 * 4. 调用 verifiedIdTokenClaims，验证能正确验签并提取 claims
 */
public class SsoManageSignatureVerificationTest {

    @Test
    public void validSignedTokenIsAccepted() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String kid = "test-key-1";
        String issuer = "https://idp.example.com";
        String clientId = "crest-client";
        String nonce = "test-nonce-123";

        String jwksJson = buildJwksJson(publicKey, kid);

        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
        String idToken = JWT.create()
                .withIssuer(issuer)
                .withAudience(clientId)
                .withKeyId(kid)
                .withClaim("nonce", nonce)
                .withClaim("sub", "user-123")
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(algorithm);

        SsoConfigVO config = new SsoConfigVO();
        config.setClientId(clientId);
        config.setIssuer(issuer);
        config.setJwksUri("https://idp.example.com/.well-known/jwks.json");

        try (MockedStatic<io.crest.utils.HttpClientUtil> mocked = mockStatic(io.crest.utils.HttpClientUtil.class)) {
            mocked.when(() -> io.crest.utils.HttpClientUtil.get(anyString(), any(HttpClientConfig.class)))
                    .thenReturn(jwksJson);

            Map<String, Object> claims = invokeVerifiedIdTokenClaims(idToken, config, newSsoState("/workbranch/index", nonce));

            assertEquals("user-123", String.valueOf(claims.get("sub")));
            assertEquals(nonce, String.valueOf(claims.get("nonce")));
        }
    }

    @Test
    public void tamperedTokenSignatureIsRejected() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String kid = "test-key-1";
        String issuer = "https://idp.example.com";
        String clientId = "crest-client";
        String nonce = "test-nonce-123";

        String jwksJson = buildJwksJson(publicKey, kid);

        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
        String idToken = JWT.create()
                .withIssuer(issuer)
                .withAudience(clientId)
                .withKeyId(kid)
                .withClaim("nonce", nonce)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(algorithm);

        // 篡改 token payload（替换 nonce）
        String[] parts = idToken.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"https://idp.example.com\",\"aud\":\"crest-client\",\"kid\":\"test-key-1\",\"nonce\":\"tampered\",\"exp\":9999999999}"
                        .getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        SsoConfigVO config = new SsoConfigVO();
        config.setClientId(clientId);
        config.setIssuer(issuer);
        config.setJwksUri("https://idp.example.com/.well-known/jwks.json");

        try (MockedStatic<io.crest.utils.HttpClientUtil> mocked = mockStatic(io.crest.utils.HttpClientUtil.class)) {
            mocked.when(() -> io.crest.utils.HttpClientUtil.get(anyString(), any(HttpClientConfig.class)))
                    .thenReturn(jwksJson);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> invokeVerifiedIdTokenClaims(tamperedToken, config, newSsoState("/workbranch/index", nonce)));
            assertTrue(ex.getMessage().contains("签名验证失败"));
        }
    }

    @Test
    public void tokenSignedByDifferentKeyIsRejected() throws Exception {
        KeyPair keyPair1 = generateRsaKeyPair();
        KeyPair keyPair2 = generateRsaKeyPair();
        RSAPublicKey pub1 = (RSAPublicKey) keyPair1.getPublic();
        RSAPrivateKey priv1 = (RSAPrivateKey) keyPair1.getPrivate();
        RSAPublicKey pub2 = (RSAPublicKey) keyPair2.getPublic();
        RSAPrivateKey priv2 = (RSAPrivateKey) keyPair2.getPrivate();

        String kid = "test-key-1";
        String issuer = "https://idp.example.com";
        String clientId = "crest-client";
        String nonce = "test-nonce-123";

        // JWKS 里放 pub1，token 用 priv2 签名
        String jwksJson = buildJwksJson(pub1, kid);
        Algorithm algorithm = Algorithm.RSA256(pub2, priv2);
        String idToken = JWT.create()
                .withIssuer(issuer)
                .withAudience(clientId)
                .withKeyId(kid)
                .withClaim("nonce", nonce)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600_000))
                .sign(algorithm);

        SsoConfigVO config = new SsoConfigVO();
        config.setClientId(clientId);
        config.setIssuer(issuer);
        config.setJwksUri("https://idp.example.com/.well-known/jwks.json");

        try (MockedStatic<io.crest.utils.HttpClientUtil> mocked = mockStatic(io.crest.utils.HttpClientUtil.class)) {
            mocked.when(() -> io.crest.utils.HttpClientUtil.get(anyString(), any(HttpClientConfig.class)))
                    .thenReturn(jwksJson);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> invokeVerifiedIdTokenClaims(idToken, config, newSsoState("/workbranch/index", nonce)));
            assertTrue(ex.getMessage().contains("签名验证失败"));
        }
    }

    @Test
    public void expiredTokenIsRejected() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String kid = "test-key-1";
        String issuer = "https://idp.example.com";
        String clientId = "crest-client";
        String nonce = "test-nonce-123";

        String jwksJson = buildJwksJson(publicKey, kid);

        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
        String idToken = JWT.create()
                .withIssuer(issuer)
                .withAudience(clientId)
                .withKeyId(kid)
                .withClaim("nonce", nonce)
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .sign(algorithm);

        SsoConfigVO config = new SsoConfigVO();
        config.setClientId(clientId);
        config.setIssuer(issuer);
        config.setJwksUri("https://idp.example.com/.well-known/jwks.json");

        try (MockedStatic<io.crest.utils.HttpClientUtil> mocked = mockStatic(io.crest.utils.HttpClientUtil.class)) {
            mocked.when(() -> io.crest.utils.HttpClientUtil.get(anyString(), any(HttpClientConfig.class)))
                    .thenReturn(jwksJson);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> invokeVerifiedIdTokenClaims(idToken, config, newSsoState("/workbranch/index", nonce)));
            assertTrue(ex.getMessage().contains("签名验证失败"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeVerifiedIdTokenClaims(String idToken, SsoConfigVO config, Object ssoState) throws Exception {
        SsoManage manage = new SsoManage();
        Class<?> ssoStateClass = findSsoStateClass();
        Method method = SsoManage.class.getDeclaredMethod("verifiedIdTokenClaims", String.class, SsoConfigVO.class, ssoStateClass);
        method.setAccessible(true);
        try {
            return (Map<String, Object>) method.invoke(manage, idToken, config, ssoState);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }

    private Object newSsoState(String redirect, String nonce) throws Exception {
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

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String buildJwksJson(RSAPublicKey publicKey, String kid) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String n = encoder.encodeToString(publicKey.getModulus().toByteArray());
        String e = encoder.encodeToString(publicKey.getPublicExponent().toByteArray());
        return """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "use": "sig",
                      "alg": "RS256",
                      "kid": "%s",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }""".formatted(kid, n, e);
    }
}
