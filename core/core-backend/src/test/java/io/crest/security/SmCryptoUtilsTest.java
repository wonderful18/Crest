package io.crest.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.utils.CommonBeanFactory;
import io.crest.utils.SignedTokenUtils;
import io.crest.utils.SmCryptoUtils;
import io.crest.utils.TokenUtils;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmCryptoUtilsTest {

    private static final String TEST_SM4_KEY = "01234567" + "89abcdef";

    @AfterEach
    void clearCryptoMode() {
        System.clearProperty("crest.crypto.mode");
        System.clearProperty("crest.crypto.sm4-key");
        new CommonBeanFactory().setApplicationContext(null);
    }

    @Test
    void shouldEncryptAndDecryptSm2CredentialPayload() {
        SmCryptoUtils.Sm2KeyPair keyPair = SmCryptoUtils.generateSm2KeyPair();

        String ciphertext = SmCryptoUtils.sm2Encrypt("Admin@123", keyPair.publicKey());

        assertTrue(ciphertext.startsWith("sm-suite:v1:sm2:"));
        assertEquals("Admin@123", SmCryptoUtils.sm2Decrypt(ciphertext, keyPair.privateKey()));
    }

    @Test
    void shouldEncryptSm4PayloadWithRandomIvAndVersionPrefix() {
        String key = TEST_SM4_KEY;

        String first = SmCryptoUtils.sm4Encrypt("{\"host\":\"127.0.0.1\"}", key);
        String second = SmCryptoUtils.sm4Encrypt("{\"host\":\"127.0.0.1\"}", key);

        assertTrue(first.startsWith("sm-suite:v1:sm4:"));
        assertEquals(3, first.substring("sm-suite:v1:sm4:".length()).split(":").length);
        assertNotEquals(first, second);
        assertEquals("{\"host\":\"127.0.0.1\"}", SmCryptoUtils.sm4Decrypt(first, key));
    }

    @Test
    void shouldRejectTamperedSm4PayloadBeforeDecrypting() {
        String key = TEST_SM4_KEY;
        String ciphertext = SmCryptoUtils.sm4Encrypt("{\"token\":\"sensitive\"}", key);
        String payload = ciphertext.substring("sm-suite:v1:sm4:".length());
        char replacement = payload.charAt(0) == '0' ? '1' : '0';
        String tamperedIv = replacement + payload.substring(1);

        assertThrows(IllegalArgumentException.class, () -> SmCryptoUtils.sm4Decrypt("sm-suite:v1:sm4:" + tamperedIv, key));
    }

    @Test
    void shouldNotTreatLegacyBase64PayloadAsSmSuiteCiphertext() {
        String legacyPayload = Base64.getEncoder().encodeToString("legacy".getBytes(StandardCharsets.UTF_8));

        assertFalse(SmCryptoUtils.isSmSuiteCiphertext(legacyPayload));
    }

    @Test
    void shouldSignAndVerifyTokenWithSm3HmacInSmSuiteMode() {
        System.setProperty("crest.crypto.mode", "sm-suite");

        String token = SignedTokenUtils.sign(Map.of("uid", 1L, "oid", 1L), "secret", new Date(System.currentTimeMillis() + 60000));
        DecodedJWT decodedJWT = SignedTokenUtils.verify(token, "secret");

        assertEquals("SM3-HMAC", decodedJWT.getAlgorithm());
        assertEquals(1L, decodedJWT.getClaim("uid").asLong());
        assertEquals(1L, decodedJWT.getClaim("oid").asLong());
    }

    @Test
    void tokenValidationUsesCurrentUserSecret() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean("crestUserManage")).thenReturn(new UserSecretManage("new-secret"));
        new CommonBeanFactory().setApplicationContext(applicationContext);
        String token = SignedTokenUtils.sign(
                Map.of("uid", 7L, "oid", 1L),
                "old-secret",
                new Date(System.currentTimeMillis() + 60_000)
        );

        assertThrows(JWTVerificationException.class, () -> TokenUtils.validate(token));
    }

    @Test
    void shouldHashCharPasswordWithStableSm3HmacOutput() {
        char[] password = "密钥Secret1".toCharArray();
        char[] original = Arrays.copyOf(password, password.length);
        byte[] salt = "salt".getBytes(StandardCharsets.UTF_8);

        byte[] first = SmCryptoUtils.sm3HmacHash(password, salt, 2);
        byte[] second = SmCryptoUtils.sm3HmacHash(password, salt, 2);

        assertArrayEquals(first, second);
        assertArrayEquals(original, password);
    }

    @Test
    void shouldValidateSmSuiteConfigurationAtStartup() {
        System.setProperty("crest.crypto.mode", "sm-suite");
        System.setProperty("crest.crypto.sm4-key", TEST_SM4_KEY);

        assertDoesNotThrow(SmCryptoUtils::validateConfiguration);
    }

    @Test
    void shouldRejectInvalidSmSuiteKeyAtStartup() {
        System.setProperty("crest.crypto.mode", "sm-suite");
        System.setProperty("crest.crypto.sm4-key", "short");

        assertThrows(IllegalStateException.class, SmCryptoUtils::validateConfiguration);
    }

    @Test
    void shouldEncryptSm2PrivateKeyBeforeStorage() {
        System.setProperty("crest.crypto.mode", "sm-suite");
        System.setProperty("crest.crypto.sm4-key", TEST_SM4_KEY);
        SmCryptoUtils.Sm2KeyPair keyPair = SmCryptoUtils.generateSm2KeyPair();

        String stored = SmCryptoUtils.encryptSm2PrivateKeyForStorage(keyPair.privateKey());

        assertTrue(stored.startsWith("sm-suite:v1:sm4:"));
        assertEquals(keyPair.privateKey(), SmCryptoUtils.readSm2PrivateKeyFromStorage(stored));
    }

    @Test
    void shouldReadLegacyPlainSm2PrivateKeyFromStorage() {
        SmCryptoUtils.Sm2KeyPair keyPair = SmCryptoUtils.generateSm2KeyPair();

        assertEquals(keyPair.privateKey(), SmCryptoUtils.readSm2PrivateKeyFromStorage(keyPair.privateKey()));
    }

    public static class UserSecretManage {
        private final String secret;

        UserSecretManage(String secret) {
            this.secret = secret;
        }

        public String secretByUid(Long uid) {
            return secret;
        }
    }

    @Test
    void shouldMatchSm3StandardDigestVector() {
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        SM3Digest digest = new SM3Digest();
        digest.update(input, 0, input.length);
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);

        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", Hex.toHexString(output));
    }

    @Test
    void shouldMatchSm4StandardBlockCipherVector() throws Exception {
        SmCryptoUtils.ensureProvider();
        Cipher cipher = Cipher.getInstance("SM4/ECB/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Hex.decode("0123456789abcdeffedcba9876543210"), "SM4"));

        byte[] ciphertext = cipher.doFinal(Hex.decode("0123456789abcdeffedcba9876543210"));

        assertEquals("681edf34d206965e86b3e94f536e4246", Hex.toHexString(ciphertext));
        assertNotNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));
    }
}
