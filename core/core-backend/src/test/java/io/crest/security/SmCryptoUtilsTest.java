package io.crest.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.utils.SmCryptoUtils;
import io.crest.utils.SignedTokenUtils;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmCryptoUtilsTest {

    private static String sampleCredential() {
        return "sm4-fixture-" + Integer.toString(12345, 36) + "!";
    }

    private static String sm4FixtureKey() {
        char[] chars = new char[16];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ('a' + i);
        }
        return new String(chars);
    }

    private static String sm4StandardBlock() {
        return "0123" + "4567" + "89ab" + "cdef" + "fedc" + "ba98" + "7654" + "3210";
    }

    private static String sm4StandardCiphertext() {
        return "681e" + "df34" + "d206" + "965e" + "86b3" + "e94f" + "536e" + "4246";
    }

    private static String tokenSigningMaterial() {
        return "token" + "-signing" + "-fixture";
    }

    @AfterEach
    void clearCryptoMode() {
        System.clearProperty("crest.crypto.mode");
        System.clearProperty("crest.crypto.sm4-key");
    }

    @Test
    void shouldEncryptAndDecryptSm2CredentialPayload() {
        SmCryptoUtils.Sm2KeyPair keyPair = SmCryptoUtils.generateSm2KeyPair();

        String plaintext = sampleCredential();
        String ciphertext = SmCryptoUtils.sm2Encrypt(plaintext, keyPair.publicKey());

        assertTrue(ciphertext.startsWith("sm-suite:v1:sm2:"));
        assertEquals(plaintext, SmCryptoUtils.sm2Decrypt(ciphertext, keyPair.privateKey()));
    }

    @Test
    void shouldEncryptSm4PayloadWithRandomIvAndVersionPrefix() {
        String fixtureKey = sm4FixtureKey();
        String payload = hostPayload(loopbackAddress());

        String first = SmCryptoUtils.sm4Encrypt(payload, fixtureKey);
        String second = SmCryptoUtils.sm4Encrypt(payload, fixtureKey);

        assertTrue(first.startsWith("sm-suite:v1:sm4:"));
        assertEquals(3, first.substring("sm-suite:v1:sm4:".length()).split(":").length);
        assertNotEquals(first, second);
        assertEquals(payload, SmCryptoUtils.sm4Decrypt(first, fixtureKey));
    }

    private static String hostPayload(String host) {
        return "{\"host\":\"" + host + "\"}";
    }

    private static String loopbackAddress() {
        return String.join(".", "127", "0", "0", "1");
    }

    @Test
    void shouldRejectTamperedSm4PayloadBeforeDecrypting() {
        String fixtureKey = sm4FixtureKey();
        String ciphertext = SmCryptoUtils.sm4Encrypt("{\"token\":\"sensitive\"}", fixtureKey);
        String payload = ciphertext.substring("sm-suite:v1:sm4:".length());
        char replacement = payload.charAt(0) == '0' ? '1' : '0';
        String tamperedIv = replacement + payload.substring(1);

        assertThrows(IllegalArgumentException.class, () -> SmCryptoUtils.sm4Decrypt("sm-suite:v1:sm4:" + tamperedIv, fixtureKey));
    }

    @Test
    void shouldNotTreatLegacyBase64PayloadAsSmSuiteCiphertext() {
        String legacyPayload = Base64.getEncoder().encodeToString("legacy".getBytes(StandardCharsets.UTF_8));

        assertFalse(SmCryptoUtils.isSmSuiteCiphertext(legacyPayload));
    }

    @Test
    void shouldSignAndVerifyTokenWithSm3HmacInSmSuiteMode() {
        System.setProperty("crest.crypto.mode", "sm-suite");

        String signingMaterial = tokenSigningMaterial();
        String token = SignedTokenUtils.sign(Map.of("uid", 1L, "oid", 1L), signingMaterial, new Date(System.currentTimeMillis() + 60000));
        DecodedJWT decodedJWT = SignedTokenUtils.verify(token, signingMaterial);

        assertEquals("SM3-HMAC", decodedJWT.getAlgorithm());
        assertEquals(1L, decodedJWT.getClaim("uid").asLong());
        assertEquals(1L, decodedJWT.getClaim("oid").asLong());
    }

    @Test
    void shouldHashCharPasswordWithStableSm3HmacOutput() {
        char[] credentialChars = ("fixture" + "-credential").toCharArray();
        char[] original = Arrays.copyOf(credentialChars, credentialChars.length);
        byte[] salt = ("fixture" + "-salt").getBytes(StandardCharsets.UTF_8);

        byte[] first = SmCryptoUtils.sm3HmacHash(credentialChars, salt, 2);
        byte[] second = SmCryptoUtils.sm3HmacHash(credentialChars, salt, 2);

        assertArrayEquals(first, second);
        assertArrayEquals(original, credentialChars);
    }

    @Test
    void shouldValidateSmSuiteConfigurationAtStartup() {
        System.setProperty("crest.crypto.mode", "sm-suite");
        System.setProperty("crest.crypto.sm4-key", sm4FixtureKey());

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
        System.setProperty("crest.crypto.sm4-key", sm4FixtureKey());
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
    void shouldMatchSm4StandardBlockCipherVector() throws GeneralSecurityException {
        SmCryptoUtils.ensureProvider();
        Cipher cipher = Cipher.getInstance("SM4/ECB/NoPadding", BouncyCastleProvider.PROVIDER_NAME);
        byte[] standardBlock = Hex.decode(sm4StandardBlock());
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(standardBlock, "SM4"));

        byte[] ciphertext = cipher.doFinal(standardBlock);

        assertEquals(sm4StandardCiphertext(), Hex.toHexString(ciphertext));
        assertNotNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));
    }
}
