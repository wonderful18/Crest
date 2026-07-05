package io.crest.security;

import io.crest.commons.utils.EncryptUtils;
import io.crest.utils.AesUtils;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedAesCompatibilityTest {

    private static final String KEY = "0123456789abcdef";
    private static final String IV = "0000000000000000";

    @Test
    void aesUtilsEncryptsNewValuesWithRandomIvAndMac() {
        String first = AesUtils.aesEncrypt("secret", KEY, IV);
        String second = AesUtils.aesEncrypt("secret", KEY, IV);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals("secret", AesUtils.aesDecrypt(first, KEY, IV));
    }

    @Test
    void aesUtilsRejectsTamperedAuthenticatedCiphertext() {
        String encrypted = AesUtils.aesEncrypt("secret", KEY, IV);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> AesUtils.aesDecrypt(tampered, KEY, IV));
    }

    @Test
    void aesUtilsKeepsLegacyFixedIvDecryptCompatibility() throws Exception {
        assertEquals("legacy", AesUtils.aesDecrypt(legacyCiphertext("legacy"), KEY, IV));
    }

    @Test
    void encryptUtilsEncryptsNewValuesWithRandomIvAndMac() {
        String first = EncryptUtils.aesEncrypt("secret", KEY, IV);
        String second = EncryptUtils.aesEncrypt("secret", KEY, IV);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals("secret", EncryptUtils.aesDecrypt(first, KEY, IV));
    }

    @Test
    void encryptUtilsRejectsTamperedAuthenticatedCiphertext() {
        String encrypted = EncryptUtils.aesEncrypt("secret", KEY, IV);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> EncryptUtils.aesDecrypt(tampered, KEY, IV));
    }

    @Test
    void encryptUtilsKeepsLegacyFixedIvDecryptCompatibility() throws Exception {
        assertEquals("legacy", EncryptUtils.aesDecrypt(legacyCiphertext("legacy"), KEY, IV));
    }

    private String legacyCiphertext(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
