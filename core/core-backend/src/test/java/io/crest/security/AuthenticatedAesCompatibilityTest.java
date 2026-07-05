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

    private static final String AES_KEY_SAMPLE = repeatedSequence('a', 16);
    private static final String AES_IV_SAMPLE = repeatedSequence('0', 16);

    private static String repeatedSequence(char start, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (start + (i % 10));
        }
        return new String(chars);
    }

    @Test
    void aesUtilsEncryptsNewValuesWithRandomIvAndMac() {
        String plaintext = "payload";
        String first = AesUtils.aesEncrypt(plaintext, AES_KEY_SAMPLE, AES_IV_SAMPLE);
        String second = AesUtils.aesEncrypt(plaintext, AES_KEY_SAMPLE, AES_IV_SAMPLE);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals(plaintext, AesUtils.aesDecrypt(first, AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    @Test
    void aesUtilsRejectsTamperedAuthenticatedCiphertext() {
        String encrypted = AesUtils.aesEncrypt("payload", AES_KEY_SAMPLE, AES_IV_SAMPLE);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> AesUtils.aesDecrypt(tampered, AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    @Test
    void aesUtilsKeepsLegacyFixedIvDecryptCompatibility() throws Exception {
        assertEquals("legacy", AesUtils.aesDecrypt(legacyCiphertext("legacy", AES_KEY_SAMPLE, AES_IV_SAMPLE), AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    @Test
    void encryptUtilsEncryptsNewValuesWithRandomIvAndMac() {
        String plaintext = "payload";
        String first = EncryptUtils.aesEncrypt(plaintext, AES_KEY_SAMPLE, AES_IV_SAMPLE);
        String second = EncryptUtils.aesEncrypt(plaintext, AES_KEY_SAMPLE, AES_IV_SAMPLE);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals(plaintext, EncryptUtils.aesDecrypt(first, AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    @Test
    void encryptUtilsRejectsTamperedAuthenticatedCiphertext() {
        String encrypted = EncryptUtils.aesEncrypt("payload", AES_KEY_SAMPLE, AES_IV_SAMPLE);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> EncryptUtils.aesDecrypt(tampered, AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    @Test
    void encryptUtilsKeepsLegacyFixedIvDecryptCompatibility() throws Exception {
        assertEquals("legacy", EncryptUtils.aesDecrypt(legacyCiphertext("legacy", AES_KEY_SAMPLE, AES_IV_SAMPLE), AES_KEY_SAMPLE, AES_IV_SAMPLE));
    }

    private String legacyCiphertext(String plainText, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
