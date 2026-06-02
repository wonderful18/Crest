package io.crest.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AesUtils {
    private static final String AES_KEY_PROPERTY = "crest.crypto.aes-key";
    private static final String AES_IV_PROPERTY = "crest.crypto.aes-iv";
    private static final String GCM_PREFIX = "aes-gcm:v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static String configuredSecretKey() {
        String value = ConfigUtils.getConfig(AES_KEY_PROPERTY, null);
        int length = StringUtils.length(value);
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException(AES_KEY_PROPERTY + " must be 16, 24, or 32 characters");
        }
        return value;
    }

    private static String configuredIv() {
        String value = ConfigUtils.getConfig(AES_IV_PROPERTY, null);
        if (StringUtils.length(value) != 16) {
            throw new IllegalStateException(AES_IV_PROPERTY + " must be 16 characters");
        }
        return value;
    }

    public static String aesDecrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        if (src != null && src.startsWith(GCM_PREFIX)) {
            return aesDecryptGcm(src, secretKey);
        }
        return aesDecryptLegacyCbc(src, secretKey, iv);
    }

    private static String aesDecryptLegacyCbc(String src, String secretKey, String iv) {
        try {
            byte[] raw = secretKey.getBytes(UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            // codeql[java/weak-cryptographic-algorithm] Compatibility path for existing CBC ciphertext. New writes use AES-GCM.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted1 = Base64.decodeBase64(src);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original, UTF_8);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            // 解密的原字符串为非加密字符串，则直接返回原字符串
            return src;
        } catch (Exception e) {
            throw new RuntimeException("decrypt error，please check parameters", e);
        }
    }

    public static String aesEncrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        return aesEncryptGcm(src, secretKey);
    }

    private static String aesEncryptGcm(String src, String secretKey) {
        try {
            byte[] ivBytes = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes));
            byte[] encrypted = cipher.doFinal(src.getBytes(UTF_8));
            byte[] payload = new byte[ivBytes.length + encrypted.length];
            System.arraycopy(ivBytes, 0, payload, 0, ivBytes.length);
            System.arraycopy(encrypted, 0, payload, ivBytes.length, encrypted.length);
            return GCM_PREFIX + Base64.encodeBase64String(payload);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt error:", e);
        }
    }

    private static String aesDecryptGcm(String src, String secretKey) {
        try {
            byte[] payload = Base64.decodeBase64(src.substring(GCM_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("invalid AES-GCM payload");
            }
            byte[] ivBytes = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes));
            return new String(cipher.doFinal(encrypted), UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt error:", e);
        }
    }

    public static Object aesEncrypt(Object o) {

        if (o == null) {
            return null;
        }
        if (CryptoMode.isSmSuite()) {
            return SmCryptoUtils.sm4EncryptWithConfiguredKey(o.toString());
        }
        return aesEncrypt(o.toString(), configuredSecretKey(), configuredIv());
    }

    public static Object aesDecrypt(Object o) {
        if (o == null) {
            return null;
        }
        String value = o.toString();
        if (SmCryptoUtils.isSm4Ciphertext(value)) {
            return SmCryptoUtils.sm4DecryptWithConfiguredKey(value);
        }
        return aesDecrypt(value, configuredSecretKey(), configuredIv());
    }

    public static String aesEncryptWithIv(String src, String secretKey) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        return aesEncryptGcm(src, secretKey);
    }
}
