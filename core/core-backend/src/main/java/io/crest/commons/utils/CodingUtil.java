package io.crest.commons.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * 通用编码和摘要工具，提供 MD5、Base64、AES 等兼容能力。
 */
public class CodingUtil {

    public static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z" };

    /**
     * BASE64解密
     *
     * @param src 待解密的字符串
     * @return 解密后的字符串
     */
    public static String base64Decoding(String src) {
        String result = null;
        if (src != null) {
            byte[] b = Base64.decodeBase64(src);
            result = new String(b, StandardCharsets.UTF_8);
        }
        return result;
    }

    /**
     * BASE64加密
     *
     * @param src 待加密的字符串
     * @return 加密后的字符串
     */
    public static String base64Encoding(String src) {
        String result = null;
        if (src != null) {
            result = Base64.encodeBase64String(src.getBytes(StandardCharsets.UTF_8));
        }
        return result;
    }

    /**
     * AES加密
     *
     * @param src       待加密字符串
     * @param secretKey 密钥
     * @param iv        向量
     * @return 加密后字符串
     */
    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String aesEncrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }

        try {
            byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            // "算法/模式/补码方式" ECB
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(encrypted);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("AES encrypt error:", e);
        }

    }

    /**
     * AES 解密
     *
     * @param src       待解密字符串
     * @param secretKey 密钥
     * @param iv        向量
     * @return 解密后字符串
     */
    @SuppressWarnings("java/weak-cryptographic-algorithm")
    public static String aesDecrypt(String src, String secretKey, String iv) {
        if (StringUtils.isBlank(secretKey)) {
            throw new RuntimeException("secretKey is empty");
        }
        try {
            byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
            IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv1);
            byte[] encrypted1 = Base64.decodeBase64(src);
            byte[] original = cipher.doFinal(encrypted1);
            return new String(original, StandardCharsets.UTF_8);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            // 解密的原字符串为非加密字符串，则直接返回原字符串
            return src;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException("decrypt error，please check parameters", e);
        }
    }


    public static String secretKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.encodeBase64String(secretKey.getEncoded());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("generate secretKey error", e);
        }

    }

    public static boolean isNumeric(String str){
        for (int i = str.length();--i>=0;){
            if (!Character.isDigit(str.charAt(i))){
                return false;
            }
        }
        return true;
    }



    public static String shortUuid() {
        StringBuilder shortBuffer = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % 0x3E]);
        }
        return shortBuffer.toString();
    }

    public static Integer string2Integer(String str) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(str)) return null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.length() > 0 ? Integer.parseInt(sb.toString()) : null;
    }
}
