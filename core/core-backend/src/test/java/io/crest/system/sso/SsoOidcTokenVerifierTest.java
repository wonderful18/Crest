package io.crest.system.sso;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.crest.api.system.vo.SsoConfigVO;
import io.crest.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SsoOidcTokenVerifierTest {

    @Test
    void verifiesSignedIdTokenAndNonce() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String issuer = "http://localhost:19090/issuer";
        String clientId = "crest-client";
        String nonce = "nonce-123";
        String token = idToken(publicKey, privateKey, issuer, clientId, nonce);

        SsoOidcTokenVerifier verifier = new SsoOidcTokenVerifier(fetcher(issuer, jwks(publicKey)));
        Map<String, Object> claims = verifier.verify(config(issuer, clientId), token, nonce);

        assertEquals("user-001", claims.get("sub"));
        assertEquals("zhang.san", claims.get("preferred_username"));
    }

    @Test
    void rejectsNonceMismatch() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String issuer = "http://localhost:19090/issuer";
        String token = idToken(publicKey, privateKey, issuer, "crest-client", "expected-nonce");

        SsoOidcTokenVerifier verifier = new SsoOidcTokenVerifier(fetcher(issuer, jwks(publicKey)));

        assertThrows(IllegalStateException.class,
                () -> verifier.verify(config(issuer, "crest-client"), token, "other-nonce"));
    }

    private Function<String, String> fetcher(String issuer, String jwks) {
        String discoveryUrl = issuer + "/.well-known/openid-configuration";
        String jwksUrl = issuer + "/protocol/openid-connect/certs";
        return url -> {
            if (discoveryUrl.equals(url)) {
                return json(Map.of("jwks_uri", jwksUrl));
            }
            if (jwksUrl.equals(url)) {
                return jwks;
            }
            throw new IllegalArgumentException("unexpected url: " + url);
        };
    }

    private SsoConfigVO config(String issuer, String clientId) {
        SsoConfigVO config = new SsoConfigVO();
        config.setIssuer(issuer);
        config.setClientId(clientId);
        config.setRequireHttps(false);
        return config;
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String idToken(RSAPublicKey publicKey, RSAPrivateKey privateKey, String issuer, String audience, String nonce) {
        return JWT.create()
                .withHeader(Map.of("kid", "test-key"))
                .withIssuer(issuer)
                .withAudience(audience)
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000L))
                .withClaim("nonce", nonce)
                .withClaim("sub", "user-001")
                .withClaim("preferred_username", "zhang.san")
                .sign(Algorithm.RSA256(publicKey, privateKey));
    }

    private String jwks(RSAPublicKey publicKey) {
        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "kid", "test-key",
                "alg", "RS256",
                "n", base64Url(publicKey.getModulus().toByteArray()),
                "e", base64Url(publicKey.getPublicExponent().toByteArray())
        );
        return json(Map.of("keys", List.of(key)));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String json(Object value) {
        return JsonUtil.toJSONString(value).toString();
    }
}
