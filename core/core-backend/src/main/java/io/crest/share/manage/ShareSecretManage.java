package io.crest.share.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.exception.CrestException;
import io.crest.share.dao.auto.entity.CoreShare;
import io.crest.share.dao.auto.mapper.CoreShareMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component("shareSecretManage")
public class ShareSecretManage {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Value("${crest.default-link-pwd:}")
    private String configuredFallbackLinkSecret;

    @Value("${crest.crypto.aes-key:}")
    private String aesKey;

    private volatile String runtimeFallbackLinkSecret;

    @Resource
    private CoreShareMapper coreShareMapper;

    public String getFallbackLinkSecret() {
        if (StringUtils.isNotBlank(configuredFallbackLinkSecret)) {
            return configuredFallbackLinkSecret;
        }
        if (StringUtils.isNotBlank(aesKey)) {
            return deriveFallbackLinkSecret(aesKey);
        }
        return getRuntimeFallbackLinkSecret();
    }

    public String getSecret(Long resourceId, Long uid) {
        QueryWrapper<CoreShare> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creator", uid);
        queryWrapper.eq("resource_id", resourceId);
        CoreShare coreShare = coreShareMapper.selectOne(queryWrapper);
        if (ObjectUtils.isEmpty(coreShare)) CrestException.throwException("Share resource do not exist");
        String sharePwd = coreShare.getPwd();
        return StringUtils.isNotBlank(sharePwd) ? sharePwd : getFallbackLinkSecret();
    }

    private String deriveFallbackLinkSecret(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("crest-link-token:" + source).getBytes(StandardCharsets.UTF_8));
            return "crest-link-token:" + BASE64_URL_ENCODER.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private String getRuntimeFallbackLinkSecret() {
        String current = runtimeFallbackLinkSecret;
        if (StringUtils.isNotBlank(current)) {
            return current;
        }
        synchronized (this) {
            if (StringUtils.isBlank(runtimeFallbackLinkSecret)) {
                byte[] secret = new byte[32];
                SECURE_RANDOM.nextBytes(secret);
                runtimeFallbackLinkSecret = "crest-link-token:" + BASE64_URL_ENCODER.encodeToString(secret);
            }
            return runtimeFallbackLinkSecret;
        }
    }
}
