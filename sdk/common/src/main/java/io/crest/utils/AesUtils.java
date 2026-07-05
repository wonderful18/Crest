package io.crest.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AesUtils {
    private static final String AES_KEY_PROPERTY = "crest.crypto.aes-key";
    private static final String AES_IV_PROPERTY = "crest.crypto.aes-iv";
    private static final String AES_CBC_HMAC_PREFIX = "aes-cbc-hmac:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
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

    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String aesDecrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        if (src != null && src.startsWith(AES_CBC_HMAC_PREFIX)) {
            try {
                return aesCbcHmacDecrypt(src, secretKey.getBytes(UTF_8));
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                throw new RuntimeException("decrypt error，please check parameters", e);
            }
        }
        try {
            byte[] raw = secretKey.getBytes(UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted1 = Base64.decodeBase64(src);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original, UTF_8);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            // 解密的原字符串为非加密字符串，则直接返回原字符串
            return src;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("decrypt error，please check parameters", e);
        }
    }

    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String aesEncrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }

        try {
            return aesCbcHmacEncrypt(src, secretKey.getBytes(UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("AES encrypt error:", e);
        }

    }

    private static String aesCbcHmacEncrypt(String src, byte[] key) throws GeneralSecurityException {
        byte[] iv = new byte[16];
        SECURE_RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        String ivText = Base64.encodeBase64String(iv);
        String ciphertext = Base64.encodeBase64String(cipher.doFinal(src.getBytes(UTF_8)));
        String payload = ivText + ":" + ciphertext;
        return AES_CBC_HMAC_PREFIX + payload + ":" + hmacHex(key, payload);
    }

    private static String aesCbcHmacDecrypt(String src, byte[] key) throws GeneralSecurityException {
        String payload = src.substring(AES_CBC_HMAC_PREFIX.length());
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("AES payload format is invalid");
        }
        String signedPayload = parts[0] + ":" + parts[1];
        if (!MessageDigest.isEqual(hexToBytes(hmacHex(key, signedPayload)), hexToBytes(parts[2]))) {
            throw new IllegalArgumentException("AES payload integrity check failed");
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Base64.decodeBase64(parts[0])));
        return new String(cipher.doFinal(Base64.decodeBase64(parts[1])), UTF_8);
    }

    private static String hmacHex(byte[] key, String payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        byte[] digest = mac.doFinal(payload.getBytes(UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            String h = Integer.toHexString(b & 0xff);
            if (h.length() == 1) {
                hex.append('0');
            }
            hex.append(h);
        }
        return hex.toString();
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || (hex.length() & 1) == 1) {
            throw new IllegalArgumentException("Invalid hex value");
        }
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }

    @SuppressWarnings("java/weak-cryptographic-algorithm")
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

    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String aesEncryptWithIv(String src, String secretKey) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        try {
            // 生成随机IV
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] raw = secretKey.getBytes(UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(src.getBytes(UTF_8));

            // 将IV拼接到密文前面
            byte[] ivAndCipher = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(encrypted, 0, ivAndCipher, iv.length, encrypted.length);

            return Base64.encodeBase64String(ivAndCipher);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("AES encrypt error:", e);
        }
    }
}
