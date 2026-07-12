package io.crest.share.manage;

import io.crest.share.dao.auto.entity.CoreShare;
import io.crest.share.dao.auto.mapper.CoreShareMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShareSecretManageTest {

    @Test
    void explicitFallbackLinkSecretKeepsCompatibility() {
        ShareSecretManage manage = new ShareSecretManage();
        ReflectionTestUtils.setField(manage, "configuredFallbackLinkSecret", "configured-secret");
        ReflectionTestUtils.setField(manage, "aesKey", "AesKeyForProductionDelivery123456");

        assertEquals("configured-secret", manage.getFallbackLinkSecret());
    }

    @Test
    void fallbackLinkSecretIsDerivedFromAesKeyWhenNotConfigured() {
        ShareSecretManage first = new ShareSecretManage();
        ShareSecretManage second = new ShareSecretManage();
        ReflectionTestUtils.setField(first, "aesKey", "AesKeyForProductionDelivery123456");
        ReflectionTestUtils.setField(second, "aesKey", "AesKeyForProductionDelivery123456");

        String derived = first.getFallbackLinkSecret();

        assertEquals(derived, second.getFallbackLinkSecret());
        assertTrue(derived.startsWith("crest-link-token:"));
        assertNotEquals("crest-link-pwd", derived);
    }

    @Test
    void runtimeFallbackLinkSecretIsStableWithinProcessWhenAesKeyIsMissing() {
        ShareSecretManage manage = new ShareSecretManage();

        String first = manage.getFallbackLinkSecret();
        String second = manage.getFallbackLinkSecret();

        assertEquals(first, second);
        assertTrue(first.startsWith("crest-link-token:"));
        assertNotEquals("crest-link-pwd", first);
    }

    @Test
    void shareWithoutPasswordUsesNonPublicDefaultSecret() {
        ShareSecretManage manage = new ShareSecretManage();
        ReflectionTestUtils.setField(manage, "aesKey", "AesKeyForProductionDelivery123456");
        CoreShareMapper coreShareMapper = mock(CoreShareMapper.class);
        CoreShare share = new CoreShare();
        share.setPwd("");
        when(coreShareMapper.selectOne(any())).thenReturn(share);
        ReflectionTestUtils.setField(manage, "coreShareMapper", coreShareMapper);

        String secret = manage.getSecret(100L, 1L);

        assertEquals(manage.getFallbackLinkSecret(), secret);
        assertNotEquals("crest-link-pwd", secret);
    }

    @Test
    void generateRandomSecretIsUniqueAndUrlSafe() {
        ShareSecretManage manage = new ShareSecretManage();
        String first = manage.generateRandomSecret();
        String second = manage.generateRandomSecret();

        assertTrue(first.startsWith("crest-link-token:"));
        assertTrue(second.startsWith("crest-link-token:"));
        assertNotEquals(first, second, "每次生成的分享密钥必须唯一，避免共用导致跨资源伪造");
        // 32 字节 base64url（无填充）应为 43 个字符
        String body = first.substring("crest-link-token:".length());
        assertTrue(body.matches("[A-Za-z0-9_-]{43}"), "分享密钥应为 32 字节的 base64url（无填充），共 43 字符");
        assertFalse(first.contains("+"));
        assertFalse(first.contains("/"));
        assertFalse(first.contains("="));
    }

    @Test
    void getSecretReturnsPerSharePasswordForIsolation() {
        ShareSecretManage manage = new ShareSecretManage();
        ReflectionTestUtils.setField(manage, "aesKey", "AesKeyForProductionDelivery123456");
        CoreShareMapper coreShareMapper = mock(CoreShareMapper.class);
        CoreShare share = new CoreShare();
        // 模拟 ShareManage 为无密码分享写入的随机密钥（MEDIUM-1 修复）
        String perShareKey = manage.generateRandomSecret();
        share.setPwd(perShareKey);
        when(coreShareMapper.selectOne(any())).thenReturn(share);
        ReflectionTestUtils.setField(manage, "coreShareMapper", coreShareMapper);

        String secret = manage.getSecret(100L, 1L);

        assertEquals(perShareKey, secret, "应返回该分享自身的密钥，而非全局 fallback，避免跨资源伪造");
        assertNotEquals(manage.getFallbackLinkSecret(), secret);
    }
}
