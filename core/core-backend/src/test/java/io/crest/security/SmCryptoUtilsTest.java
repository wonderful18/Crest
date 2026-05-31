package io.crest.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.utils.SmCryptoUtils;
import io.crest.utils.SignedTokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmCryptoUtilsTest {

    @AfterEach
    void clearCryptoMode() {
        System.clearProperty("crest.crypto.mode");
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
        String key = "0123456789abcdef";

        String first = SmCryptoUtils.sm4Encrypt("{\"host\":\"127.0.0.1\"}", key);
        String second = SmCryptoUtils.sm4Encrypt("{\"host\":\"127.0.0.1\"}", key);

        assertTrue(first.startsWith("sm-suite:v1:sm4:"));
        assertNotEquals(first, second);
        assertEquals("{\"host\":\"127.0.0.1\"}", SmCryptoUtils.sm4Decrypt(first, key));
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
}
