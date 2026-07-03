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
// 提供 RSA、SM2 和对称加密相关的统一工具方法
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

    // 注入 RSA 密钥管理服务
    @Resource
    public void setRsaManage(RsaManage rsaManage) {
        RsaUtils.rsaManage = rsaManage;
    }

    // 生成 RSA 密钥对
    private static KeyPair getKeyPair() {
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(e);
            return null;
        }
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // 从 Base64 文本恢复 RSA 私钥
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

    // 从 Base64 文本恢复 RSA 公钥
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

    // 使用 RSA 公钥分段加密明文
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

    // 使用 RSA 私钥分段解密密文
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

    // 生成 RSA 密钥和 AES 密钥组合
    public static RSAModel generate() {
        KeyPair keyPair = getKeyPair();
        if (keyPair == null) {
            CrestException.throwException("RSA key pair generate failed");
            return null;
        }
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        RSAModel rsaModel = new RSAModel();
        rsaModel.setPrivateKey(privateKey);
        rsaModel.setPublicKey(publicKey);
        rsaModel.setAesKey(generateAesKey());
        return rsaModel;
    }

    // 使用指定私钥解密密文字符串
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

    // 使用当前系统私钥解密密文字符串
    public static String decryptStr(String data) {
        if (SmCryptoUtils.isSm2Ciphertext(data)) {
            CoreRsa coreRsa = rsaManage.ensureSmSuiteKey();
            return decryptStr(data, SmCryptoUtils.readSm2PrivateKeyFromStorage(coreRsa.getSm2PrivateKey()));
        }
        return decryptStr(data, privateKey());
    }

    // 使用当前系统公钥加密字符串
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

    // 查询当前 RSA 私钥
    public static String privateKey() {
        CoreRsa coreRsa = rsaManage.query();
        return coreRsa.getPrivateKey();
    }

    // 查询前端可用的公钥载荷
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
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 生成 AES 密钥文本
    private static String generateAesKey() {
        return RandomStringUtils.secure().nextAlphanumeric(16);
    }

    // 使用 AES 加密公钥载荷
    @SuppressWarnings("java/weak-cryptographic-algorithm")
    private static String ascEncrypt(String message, String key) {
        try {
            byte[] baseKey = key.getBytes(StandardCharsets.UTF_8);
            return aesCtrHmacEncrypt(message, baseKey);
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }


    private static final String ALGORITHM = "AES";
    private static final String SYMMETRIC_KEY_CACHE_ENTRY = "symmetricKey";
    private static final int KEY_SIZE = 128;


    // 生成或读取缓存中的对称密钥
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

    // 使用当前对称密钥加密字符串
    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String symmetricEncrypt(String data) {
        if (CryptoMode.isSmSuite()) {
            return SmCryptoUtils.sm4EncryptWithBase64Key(data, generateSymmetricKey());
        }
        return symmetricEncrypt(data, generateSymmetricKey());
    }

    static String symmetricEncrypt(String data, String base64Key) {
        try {
            return aesCtrHmacEncrypt(data, Base64.getDecoder().decode(base64Key));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 使用当前对称密钥解密字符串
    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String symmetricDecrypt(String data) {
        if (SmCryptoUtils.isSm4Ciphertext(data)) {
            return SmCryptoUtils.sm4DecryptWithBase64Key(data, generateSymmetricKey());
        }
        return symmetricDecrypt(data, generateSymmetricKey());
    }

    static String symmetricDecrypt(String data, String base64Key) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Key);
            if (data != null && data.startsWith(AES_CTR_HMAC_PREFIX)) {
                return aesCtrHmacDecrypt(data, key);
            }
            byte[] iv = IV_KEY.getBytes(StandardCharsets.UTF_8);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decodedCiphertext = Base64.getDecoder().decode(data);
            byte[] decryptedText = cipher.doFinal(decodedCiphertext);
            return new String(decryptedText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String aesCtrHmacEncrypt(String data, byte[] key) throws Exception {
        byte[] iv = new byte[16];
        SECURE_RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, ALGORITHM), new IvParameterSpec(iv));
        String ivText = Base64.getEncoder().encodeToString(iv);
        String ciphertext = Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        String payload = ivText + ":" + ciphertext;
        return AES_CTR_HMAC_PREFIX + payload + ":" + hmacHex(key, payload);
    }

    private static String aesCtrHmacDecrypt(String data, byte[] key) throws Exception {
        String payload = data.substring(AES_CTR_HMAC_PREFIX.length());
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            CrestException.throwException(ResultCode.PARAM_IS_INVALID.code(), "加密参数格式无效");
        }
        String signedPayload = parts[0] + ":" + parts[1];
        if (!MessageDigest.isEqual(hexToBytes(hmacHex(key, signedPayload)), hexToBytes(parts[2]))) {
            CrestException.throwException(ResultCode.PARAM_IS_INVALID.code(), "加密参数格式无效");
        }
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, ALGORITHM), new IvParameterSpec(Base64.getDecoder().decode(parts[0])));
        return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
    }

    private static String hmacHex(byte[] key, String payload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
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

    private static byte[] hexToBytes(String value) {
        if (value == null || value.length() % 2 != 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                return new byte[0];
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }
}
