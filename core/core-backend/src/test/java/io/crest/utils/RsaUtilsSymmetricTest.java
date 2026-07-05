package io.crest.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsaUtilsSymmetricTest {

    private static final String AES_KEY_SAMPLE = Base64.getEncoder().encodeToString(testKeyMaterial().getBytes(StandardCharsets.UTF_8));

    private static String testKeyMaterial() {
        char[] chars = new char[16];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('a' + i);
        }
        return new String(chars);
    }

    private static String zeroIv() {
        return "0".repeat(16);
    }

    @Test
    @DisplayName("对称加密应使用随机 IV 并携带完整性校验")
    void shouldEncryptWithRandomIvAndMac() {
        String credentialField = String.valueOf(new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
        String payload = "{\"" + credentialField + "\":\"fixture\"}";
        String first = RsaUtils.symmetricEncrypt(payload, AES_KEY_SAMPLE);
        String second = RsaUtils.symmetricEncrypt(payload, AES_KEY_SAMPLE);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals(payload, RsaUtils.symmetricDecrypt(first, AES_KEY_SAMPLE));
    }

    @Test
    @DisplayName("对称密文被篡改时应拒绝解密")
    void shouldRejectTamperedCiphertext() {
        String encrypted = RsaUtils.symmetricEncrypt("payload", AES_KEY_SAMPLE);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> RsaUtils.symmetricDecrypt(tampered, AES_KEY_SAMPLE));
    }

    @Test
    @DisplayName("旧版固定 IV 密文应保持兼容解密")
    void shouldDecryptLegacyCiphertext() throws GeneralSecurityException {
        byte[] key = Base64.getDecoder().decode(AES_KEY_SAMPLE);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(zeroIv().getBytes(StandardCharsets.UTF_8)));
        String legacy = Base64.getEncoder().encodeToString(cipher.doFinal("legacy".getBytes(StandardCharsets.UTF_8)));

        assertEquals("legacy", RsaUtils.symmetricDecrypt(legacy, AES_KEY_SAMPLE));
    }
}
