package io.crest.security;

import io.crest.utils.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private static String sampleCredential() {
        return "fixture-" + Long.toString(0xC0FFEE, 36) + "!";
    }

    private static String wrongCredential() {
        return "fixture-" + Long.toString(0xBAD, 36);
    }

    @org.junit.jupiter.api.AfterEach
    void clearCryptoMode() {
        System.clearProperty("crest.crypto.mode");
    }

    @Test
    @DisplayName("应正确编码密码")
    void shouldEncodePassword() {
        String encoded = PasswordEncoder.encode(sampleCredential());
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        // 格式应为：iterations:salt:hash
        String[] parts = encoded.split(":");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("相同密码应产生不同的哈希（随机盐）")
    void shouldProduceDifferentHashesForSamePassword() {
        String encoded1 = PasswordEncoder.encode(sampleCredential());
        String encoded2 = PasswordEncoder.encode(sampleCredential());
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    @DisplayName("应正确验证密码")
    void shouldVerifyPassword() {
        String credential = sampleCredential();
        String encoded = PasswordEncoder.encode(credential);
        assertTrue(PasswordEncoder.matches(credential, encoded));
    }

    @Test
    @DisplayName("错误密码应验证失败")
    void shouldRejectWrongCredential() {
        String encoded = PasswordEncoder.encode(sampleCredential());
        assertFalse(PasswordEncoder.matches(wrongCredential(), encoded));
    }

    @Test
    @DisplayName("null 密码应验证失败")
    void shouldHandleNullPassword() {
        String encoded = PasswordEncoder.encode(sampleCredential());
        assertFalse(PasswordEncoder.matches(null, encoded));
        assertFalse(PasswordEncoder.matches("Test", null));
    }

    @Test
    @DisplayName("应检测 MD5 格式需要升级")
    void shouldDetectMd5NeedsUpgrade() {
        // MD5 格式：32 位十六进制
        String md5Hash = "e10adc3949ba59abbe56e057f20f883e";
        assertTrue(PasswordEncoder.needsReEncoding(md5Hash));
    }

    @Test
    @DisplayName("应检测新格式不需要升级")
    void shouldDetectNewFormatNoUpgrade() {
        String encoded = PasswordEncoder.encode(sampleCredential());
        assertFalse(PasswordEncoder.needsReEncoding(encoded));
    }

    @Test
    @DisplayName("null 应需要升级")
    void shouldDetectNullNeedsUpgrade() {
        assertTrue(PasswordEncoder.needsReEncoding(null));
    }

    @Test
    @DisplayName("null 密码编码应抛出异常")
    void shouldThrowOnNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.encode(null));
    }

    @Test
    @DisplayName("SM 算法套件模式应使用 SM3 迭代哈希并兼容验证")
    void shouldUseSm3HashingInSmSuiteMode() {
        System.setProperty("crest.crypto.mode", "sm-suite");

        String encoded = PasswordEncoder.encode(sampleCredential());

        assertTrue(encoded.startsWith("sm3:"));
        assertTrue(PasswordEncoder.matches(sampleCredential(), encoded));
        assertFalse(PasswordEncoder.matches(wrongCredential(), encoded));
        assertFalse(PasswordEncoder.needsReEncoding(encoded));
    }

    @Test
    @DisplayName("SM 算法套件模式应识别旧 PBKDF2 密码需要升级")
    void shouldUpgradeLegacyPbkdf2HashInSmSuiteMode() {
        String legacyEncoded = PasswordEncoder.encode(sampleCredential());
        System.setProperty("crest.crypto.mode", "sm-suite");

        assertTrue(PasswordEncoder.matches(sampleCredential(), legacyEncoded));
        assertTrue(PasswordEncoder.needsReEncoding(legacyEncoded));
    }
}
