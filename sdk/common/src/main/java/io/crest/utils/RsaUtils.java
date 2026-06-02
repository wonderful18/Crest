package io.crest.utils;


import io.crest.exception.CrestException;
import io.crest.model.RSAModel;
import io.crest.result.ResultCode;
import io.crest.rsa.dao.entity.CoreRsa;
import io.crest.rsa.manage.RsaManage;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static io.crest.constant.CacheConstant.UserCacheConstant.Symmetric_Key;

@Component
public class RsaUtils {

    static {
        if (ObjectUtils.isNotEmpty(Security.getProvider("BC"))) {
            Security.removeProvider("BC");
        }
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    private static final int MAX_ENCRYPT_BLOCK = 245;

    private static final int MAX_DECRYPT_BLOCK = 256;

    private static final String PK_SEPARATOR = "-pk_separator-";

    private static RsaManage rsaManage;

    @Resource
    public void setRsaManage(RsaManage rsaManage) {
        RsaUtils.rsaManage = rsaManage;
    }

    private static KeyPair getKeyPair() {
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(e);
        }
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static PrivateKey getPrivateKey(String privateKey) {
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = Base64.getDecoder().decode(privateKey.getBytes());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static PublicKey getPublicKey(String publicKey) {
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = Base64.getDecoder().decode(publicKey.getBytes());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        int inputLen = data.getBytes().length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data.getBytes(), offset, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data.getBytes(), offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    private static String decrypt(String data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] dataBytes = Base64.getDecoder().decode(data);
        int inputLen = dataBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offset = 0;
        byte[] cache;
        int i = 0;
        while (inputLen - offset > 0) {
            if (inputLen - offset > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(dataBytes, offset, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(dataBytes, offset, inputLen - offset);
            }
            out.write(cache, 0, cache.length);
            i++;
            offset = i * MAX_DECRYPT_BLOCK;
        }
        out.close();
        return out.toString(StandardCharsets.UTF_8);
    }

    public static RSAModel generate() {
        KeyPair keyPair = getKeyPair();
        String privateKey = new String(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded()));
        String publicKey = new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded()));
        RSAModel rsaModel = new RSAModel();
        rsaModel.setPrivateKey(privateKey);
        rsaModel.setPublicKey(publicKey);
        rsaModel.setAesKey(generateAesKey());
        return rsaModel;
    }

    public static String decryptStr(String data, String privateKey) {
        if (StringUtils.isBlank(data) || StringUtils.isBlank(privateKey)) {
            CrestException.throwException(ResultCode.PARAM_IS_INVALID.code(), "加密参数格式无效");
        }
        if (SmCryptoUtils.isSm2Ciphertext(data)) {
            try {
                return SmCryptoUtils.sm2Decrypt(data, privateKey);
            } catch (Exception e) {
                LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
                CrestException.throwException(ResultCode.PARAM_IS_INVALID.code(), "加密参数格式无效");
                return "";
            }
        }
        try {
            return decrypt(data, getPrivateKey(privateKey));
        } catch (Exception e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            CrestException.throwException(ResultCode.PARAM_IS_INVALID.code(), "加密参数格式无效");
            return "";
        }
    }

    public static String decryptStr(String data) {
        if (SmCryptoUtils.isSm2Ciphertext(data)) {
            CoreRsa coreRsa = rsaManage.ensureSmSuiteKey();
            return decryptStr(data, SmCryptoUtils.readSm2PrivateKeyFromStorage(coreRsa.getSm2PrivateKey()));
        }
        return decryptStr(data, privateKey());
    }

    public static String encryptStr(String data) {
        if (CryptoMode.isSmSuite()) {
            CoreRsa coreRsa = rsaManage.ensureSmSuiteKey();
            return SmCryptoUtils.sm2Encrypt(data, coreRsa.getSm2PublicKey());
        }
        try {
            return encrypt(data, getPublicKey(publicKey()));
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static String privateKey() {
        CoreRsa coreRsa = rsaManage.query();
        return coreRsa.getPrivateKey();
    }

    public static String publicKey() {
        if (CryptoMode.isSmSuite()) {
            CoreRsa coreRsa = rsaManage.ensureSmSuiteKey();
            return SmCryptoUtils.sm2PublicKeyPayload(coreRsa.getSm2PublicKey());
        }
        CoreRsa coreRsa = rsaManage.query();
        String publicKey = coreRsa.getPublicKey();
        String aesKey = coreRsa.getAesKey();
        String pk = ascEncrypt(publicKey, aesKey).replaceAll("[\\s*\t\n\r]", "");
        String separator = Base64.getUrlEncoder().encodeToString(PK_SEPARATOR.getBytes(StandardCharsets.UTF_8));
        return pk + separator + aesKey;
    }

    public static final String IV_KEY = "0000000000000000";
    private static final String AES_CTR_HMAC_PREFIX = "aes-ctr-hmac:v1:";
    private static final int CTR_IV_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static String generateAesKey() {
        return RandomStringUtils.secure().nextAlphanumeric(16);
    }

    private static String ascEncrypt(String message, String key) {
        return aesCtrHmacEncrypt(message, key.getBytes(StandardCharsets.UTF_8));
    }


    private static final String ALGORITHM = "AES";
    private static final String SYMMETRIC_KEY_CACHE_ENTRY = "symmetricKey";
    private static final int KEY_SIZE = 128;


    public static String generateSymmetricKey() {
        try {
            if (!CacheUtils.keyExist(Symmetric_Key, SYMMETRIC_KEY_CACHE_ENTRY)) {
                if (CryptoMode.isSmSuite()) {
                    CacheUtils.put(Symmetric_Key, SYMMETRIC_KEY_CACHE_ENTRY, SmCryptoUtils.generateBase64Sm4Key());
                } else {
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
                    keyGenerator.init(KEY_SIZE, new SecureRandom());
                    SecretKey secretKey = keyGenerator.generateKey();
                    CacheUtils.put(Symmetric_Key, SYMMETRIC_KEY_CACHE_ENTRY, Base64.getEncoder().encodeToString(secretKey.getEncoded()));
                }
            }
            return CacheUtils.get(Symmetric_Key, SYMMETRIC_KEY_CACHE_ENTRY).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String symmetricEncrypt(String data) {
        if (CryptoMode.isSmSuite()) {
            return SmCryptoUtils.sm4EncryptWithBase64Key(data, generateSymmetricKey());
        }
        return aesCtrHmacEncrypt(data, Base64.getDecoder().decode(generateSymmetricKey()));
    }

    private static String aesCtrHmacEncrypt(String data, byte[] keyBytes) {
        try {
            byte[] iv = new byte[CTR_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM), new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String ivText = Base64.getUrlEncoder().withoutPadding().encodeToString(iv);
            String ciphertextText = Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
            String mac = hmacHex(ivText + ":" + ciphertextText, keyBytes);
            return AES_CTR_HMAC_PREFIX + ivText + ":" + ciphertextText + ":" + mac;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String symmetricDecrypt(String data) {
        if (SmCryptoUtils.isSm4Ciphertext(data)) {
            return SmCryptoUtils.sm4DecryptWithBase64Key(data, generateSymmetricKey());
        }
        if (data != null && data.startsWith(AES_CTR_HMAC_PREFIX)) {
            return aesCtrHmacDecrypt(data, Base64.getDecoder().decode(generateSymmetricKey()));
        }
        return symmetricDecryptLegacyCbc(data);
    }

    private static String aesCtrHmacDecrypt(String data, byte[] keyBytes) {
        try {
            String[] parts = data.substring(AES_CTR_HMAC_PREFIX.length()).split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("invalid AES-CTR payload");
            }
            String expectedMac = hmacHex(parts[0] + ":" + parts[1], keyBytes);
            if (!constantTimeEquals(expectedMac, parts[2])) {
                throw new IllegalArgumentException("invalid AES-CTR payload mac");
            }
            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[1]);
            if (iv.length != CTR_IV_LENGTH) {
                throw new IllegalArgumentException("invalid AES-CTR iv");
            }
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM), new IvParameterSpec(iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String symmetricDecryptLegacyCbc(String data) {
        try {
            byte[] iv = IV_KEY.getBytes(StandardCharsets.UTF_8);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(generateSymmetricKey()), ALGORITHM);
            // codeql[java/weak-cryptographic-algorithm] Compatibility path for existing CBC ciphertext. New writes use AES-CTR with HMAC.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decodedCiphertext = Base64.getDecoder().decode(data);
            byte[] decryptedText = cipher.doFinal(decodedCiphertext);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hmacHex(String data, byte[] keyBytes) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            result.append(String.format("%02x", item & 0xff));
        }
        return result.toString();
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
