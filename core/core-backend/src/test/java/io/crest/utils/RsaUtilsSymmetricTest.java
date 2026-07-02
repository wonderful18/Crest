package io.crest.utils;

import org.junit.jupiter.api.DisplayName;
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

class RsaUtilsSymmetricTest {

    private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("对称加密应使用随机 IV 并携带完整性校验")
    void shouldEncryptWithRandomIvAndMac() {
        String first = RsaUtils.symmetricEncrypt("{\"password\":\"secret\"}", KEY);
        String second = RsaUtils.symmetricEncrypt("{\"password\":\"secret\"}", KEY);

        assertTrue(first.startsWith("aes-cbc-hmac:v1:"));
        assertNotEquals(first, second);
        assertEquals("{\"password\":\"secret\"}", RsaUtils.symmetricDecrypt(first, KEY));
    }

    @Test
    @DisplayName("对称密文被篡改时应拒绝解密")
    void shouldRejectTamperedCiphertext() {
        String encrypted = RsaUtils.symmetricEncrypt("secret", KEY);
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("0") ? "1" : "0");

        assertThrows(RuntimeException.class, () -> RsaUtils.symmetricDecrypt(tampered, KEY));
    }

    @Test
    @DisplayName("旧版固定 IV 密文应保持兼容解密")
    void shouldDecryptLegacyCiphertext() throws Exception {
        byte[] key = Base64.getDecoder().decode(KEY);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec("0000000000000000".getBytes(StandardCharsets.UTF_8)));
        String legacy = Base64.getEncoder().encodeToString(cipher.doFinal("legacy".getBytes(StandardCharsets.UTF_8)));

        assertEquals("legacy", RsaUtils.symmetricDecrypt(legacy, KEY));
    }
}
