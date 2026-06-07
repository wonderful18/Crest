package io.crest.system.sso;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.type.TypeReference;
import io.crest.api.system.vo.SsoConfigVO;
import io.crest.utils.HttpClientConfig;
import io.crest.utils.HttpClientUtil;
import io.crest.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class SsoOidcTokenVerifier {

    private static final long CLOCK_SKEW_SECONDS = 60L;

    private final Function<String, String> httpGet;

    public SsoOidcTokenVerifier() {
        this(SsoOidcTokenVerifier::getJson);
    }

    SsoOidcTokenVerifier(Function<String, String> httpGet) {
        this.httpGet = httpGet;
    }

    public Map<String, Object> verify(SsoConfigVO config, String idToken, String nonce) {
        if (StringUtils.isBlank(idToken)) {
            throw new IllegalStateException("令牌响应中缺少 id_token");
        }
        String issuer = StringUtils.trimToEmpty(config.getIssuer());
        if (issuer.isEmpty()) {
            throw new IllegalStateException("OIDC Issuer 不能为空");
        }

        // Decode only selects the JWKS key; verifier.verify() below enforces integrity and expiry.
        DecodedJWT decoded = JWT.decode(idToken);
        Algorithm algorithm = algorithm(decoded, jwks(config, issuer));
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(config.getClientId())
                .acceptLeeway(CLOCK_SKEW_SECONDS)
                .build();
        DecodedJWT verified = verifier.verify(idToken);
        String tokenNonce = verified.getClaim("nonce").asString();
        if (!Objects.equals(nonce, tokenNonce)) {
            throw new IllegalStateException("单点登录 nonce 校验失败");
        }
        return claims(verified);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> jwks(SsoConfigVO config, String issuer) {
        String discoveryEndpoint = discoveryEndpoint(issuer);
        SsoEndpointPolicy.validateEndpoint(discoveryEndpoint, config.getRequireHttps(), "OIDC Discovery 端点");
        Map<String, Object> discovery = parseObject(httpGet.apply(discoveryEndpoint), "OIDC Discovery");
        String jwksUri = asText(discovery.get("jwks_uri"));
        if (StringUtils.isBlank(jwksUri)) {
            throw new IllegalStateException("OIDC Discovery 响应缺少 jwks_uri");
        }
        SsoEndpointPolicy.validateEndpoint(jwksUri, config.getRequireHttps(), "OIDC JWKS 端点");
        Map<String, Object> jwks = parseObject(httpGet.apply(jwksUri), "OIDC JWKS");
        Object keys = jwks.get("keys");
        if (!(keys instanceof List<?> list)) {
            throw new IllegalStateException("OIDC JWKS 响应缺少 keys");
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private Algorithm algorithm(DecodedJWT decoded, List<Map<String, Object>> keys) {
        String algorithm = decoded.getAlgorithm();
        if (!"RS256".equals(algorithm) && !"RS384".equals(algorithm) && !"RS512".equals(algorithm)) {
            throw new IllegalStateException("不支持的 OIDC 签名算法：" + algorithm);
        }
        RSAPublicKey publicKey = rsaPublicKey(decoded.getKeyId(), keys);
        return switch (algorithm) {
            case "RS384" -> Algorithm.RSA384(publicKey, null);
            case "RS512" -> Algorithm.RSA512(publicKey, null);
            default -> Algorithm.RSA256(publicKey, null);
        };
    }

    private RSAPublicKey rsaPublicKey(String keyId, List<Map<String, Object>> keys) {
        List<Map<String, Object>> matches = keys.stream()
                .filter(key -> Strings.CI.equals(asText(key.get("kty")), "RSA"))
                .filter(key -> StringUtils.isBlank(asText(key.get("use"))) || Strings.CI.equals(asText(key.get("use")), "sig"))
                .filter(key -> StringUtils.isBlank(keyId) || Strings.CS.equals(keyId, asText(key.get("kid"))))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException("未找到匹配的 OIDC JWKS 签名公钥");
        }
        Map<String, Object> key = matches.get(0);
        String modulus = asText(key.get("n"));
        String exponent = asText(key.get("e"));
        if (StringUtils.isAnyBlank(modulus, exponent)) {
            throw new IllegalStateException("OIDC JWKS RSA 公钥缺少 n 或 e");
        }
        try {
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
        } catch (Exception exception) {
            throw new IllegalStateException("OIDC JWKS RSA 公钥解析失败", exception);
        }
    }

    private Map<String, Object> claims(DecodedJWT jwt) {
        String payload = new String(Base64.getUrlDecoder().decode(jwt.getPayload()), StandardCharsets.UTF_8);
        Map<String, Object> claims = JsonUtil.parseObject(payload, new TypeReference<>() {});
        if (claims == null) {
            throw new IllegalStateException("OIDC id_token 载荷不是有效 JSON");
        }
        return claims;
    }

    private Map<String, Object> parseObject(String json, String name) {
        Map<String, Object> result = JsonUtil.parseObject(json, new TypeReference<>() {});
        if (result == null) {
            throw new IllegalStateException(name + " 响应不是有效 JSON");
        }
        return result;
    }

    private String discoveryEndpoint(String issuer) {
        try {
            URI uri = new URI(issuer);
            String path = StringUtils.defaultString(uri.getPath());
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            path += "/.well-known/openid-configuration";
            return new URI(uri.getScheme(), uri.getAuthority(), path, null, null).toString();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("OIDC Issuer 地址无效", exception);
        }
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private static String getJson(String url) {
        HttpClientConfig config = new HttpClientConfig();
        config.addHeader("Accept", "application/json");
        return HttpClientUtil.get(url, config);
    }
}
