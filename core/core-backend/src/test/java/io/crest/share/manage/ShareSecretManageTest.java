package io.crest.share.manage;

import io.crest.share.dao.auto.entity.CoreShare;
import io.crest.share.dao.auto.mapper.CoreShareMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
