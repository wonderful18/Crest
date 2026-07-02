package io.crest.embedded.server;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.exception.CrestException;
import io.crest.utils.ServletUtils;
import io.crest.utils.TokenUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/embedded")
public class EmbeddedTokenServer {

    private static final Set<String> RESERVED_CLAIMS = Set.of(
            "aud", "exp", "iat", "iss", "jti", "nbf", "sub",
            "uid", "oid", "resourceId"
    );

    private static final Set<String> ARG_CONTAINER_CLAIMS = Set.of("args", "params", "tokenArgs");

    @GetMapping({"/token-args", "/getTokenArgs"})
    public Map<String, Object> getTokenArgs() {
        String token = ServletUtils.getEmbeddedToken();
        if (StringUtils.isBlank(token)) {
            return Map.of();
        }
        try {
            DecodedJWT jwt = TokenUtils.verifyUserToken(token);
            Map<String, Object> result = new LinkedHashMap<>();
            ARG_CONTAINER_CLAIMS.forEach(claimName -> putMapClaim(result, jwt.getClaim(claimName)));
            jwt.getClaims().forEach((key, claim) -> {
                if (!RESERVED_CLAIMS.contains(key) && !ARG_CONTAINER_CLAIMS.contains(key)) {
                    Object value = claimValue(claim);
                    if (value != null) {
                        result.put(key, value);
                    }
                }
            });
            return result;
        } catch (CrestException | JWTVerificationException e) {
            CrestException.throwException("embedded token is invalid");
            return Map.of();
        }
    }

    private void putMapClaim(Map<String, Object> target, Claim claim) {
        if (claim == null || claim.isNull()) {
            return;
        }
        Map<String, Object> value = claim.asMap();
        if (value != null) {
            target.putAll(value);
        }
    }

    private Object claimValue(Claim claim) {
        if (claim == null || claim.isNull()) {
            return null;
        }
        Map<String, Object> mapValue = claim.asMap();
        if (mapValue != null) {
            return mapValue;
        }
        Object[] arrayValue = claim.asArray(Object.class);
        if (arrayValue != null) {
            return Arrays.asList(arrayValue);
        }
        String stringValue = claim.asString();
        if (stringValue != null) {
            return stringValue;
        }
        Long longValue = claim.asLong();
        if (longValue != null) {
            return longValue;
        }
        Double doubleValue = claim.asDouble();
        if (doubleValue != null) {
            return doubleValue;
        }
        return claim.asBoolean();
    }
}
