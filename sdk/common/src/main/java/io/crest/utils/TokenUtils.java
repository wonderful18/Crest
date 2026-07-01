package io.crest.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class TokenUtils {


    public static TokenUserBO userBOByToken(String token) {
        DecodedJWT jwt = verifyUserToken(token);
        Long userId = jwt.getClaim("uid").asLong();
        Long oid = jwt.getClaim("oid").asLong();
        if (ObjectUtils.isEmpty(userId)) {
            CrestException.throwException("token格式错误！");
        }
        return new TokenUserBO(userId, oid);
    }

    public static DecodedJWT verifyUserToken(String token) {
        String secret = userSecret(token);
        return SignedTokenUtils.verify(token, secret);
    }

    public static TokenUserBO validate(String token) {
        if (StringUtils.isBlank(token)) {
            String uri = ServletUtils.request().getRequestURI();
            CrestException.throwException("token is empty for uri {" + uri + "}");
        }
        if (StringUtils.length(token) < 100) {
            CrestException.throwException("token is invalid");
        }
        return userBOByToken(token);
    }

    private static String userSecret(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            Long uid = jwt.getClaim("uid").asLong();
            if (ObjectUtils.isEmpty(uid)) {
                CrestException.throwException("token格式错误！");
            }
            Object userManage = CommonBeanFactory.getBean("crestUserManage");
            if (userManage == null) {
                CrestException.throwException("token is invalid");
            }
            Method method = CrestReflectionUtils.findMethod(userManage.getClass(), "secretByUid");
            Object secret = ReflectionUtils.invokeMethod(method, userManage, uid);
            if (secret != null && StringUtils.isNotBlank(secret.toString())) {
                return secret.toString();
            }
        } catch (CrestException e) {
            throw e;
        } catch (Exception e) {
            CrestException.throwException("token is invalid");
        }
        CrestException.throwException("token is invalid");
        return null;
    }


    public static TokenUserBO validateLinkToken(String linkToken) {
        CrestException.throwException("link token must be verified by TokenFilter");
        return null;
    }
}
