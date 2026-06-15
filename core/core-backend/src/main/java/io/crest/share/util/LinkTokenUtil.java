package io.crest.share.util;

import io.crest.utils.SignedTokenUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

// 定义当前业务对象的核心职责
public class LinkTokenUtil {
    // 执行当前服务方法并返回处理结果
    public static String generate(Long uid, Long resourceId, Long exp, String pwd, Long oid) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", uid);
        claims.put("resourceId", resourceId);
        claims.put("oid", oid);
        Date expiresAt = null;
        if (ObjectUtils.isNotEmpty(exp) && !exp.equals(0L)) {
            expiresAt = new Date(exp);
        }
        return SignedTokenUtils.sign(claims, pwd, expiresAt);
    }
}
