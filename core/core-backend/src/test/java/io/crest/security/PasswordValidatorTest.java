package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.utils.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private static String strongCredential(String seed) {
        return "Aa1!" + seed + Long.toString(seed.hashCode() & 0xffffffffL, 36);
    }

    private static String weakDictionaryCredential() {
        return String.valueOf(new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
    }

    @Test
    @DisplayName("强密码应通过验证")
    void shouldAcceptStrongPassword() {
        assertDoesNotThrow(() -> PasswordValidator.validate(strongCredential("one")));
        assertDoesNotThrow(() -> PasswordValidator.validate(strongCredential("two")));
        assertDoesNotThrow(() -> PasswordValidator.validate(strongCredential("three")));
    }

    @ParameterizedTest
    @MethodSource("weakCredentials")
    @DisplayName("弱密码应被拒绝")
    void shouldRejectWeakPasswords(String password) {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(password));
    }

    @Test
    @DisplayName("缺少大写字母应被拒绝")
    void shouldRejectWithoutUppercase() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(weakDictionaryCredential() + "1!"));
    }

    @Test
    @DisplayName("缺少小写字母应被拒绝")
    void shouldRejectWithoutLowercase() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(weakDictionaryCredential().toUpperCase() + "1!"));
    }

    @Test
    @DisplayName("缺少数字应被拒绝")
    void shouldRejectWithoutDigit() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate("My" + weakDictionaryCredential() + "!"));
    }

    @Test
    @DisplayName("缺少特殊字符应被拒绝")
    void shouldRejectWithoutSpecialChar() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate("My" + weakDictionaryCredential() + "1"));
    }

    @ParameterizedTest
    @MethodSource("commonWeakCredentials")
    @DisplayName("常见弱密码应被拒绝")
    void shouldRejectCommonPasswords(String password) {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(password));
    }

    @Test
    @DisplayName("连续相同字符应被拒绝")
    void shouldRejectSequentialChars() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate("Aaa111!@#"));
    }

    @Test
    @DisplayName("null 密码应抛出异常")
    void shouldRejectNull() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(null));
    }

    @Test
    @DisplayName("空密码应抛出异常")
    void shouldRejectEmpty() {
        assertThrows(CrestException.class, () -> PasswordValidator.validate(""));
    }

    static Stream<String> weakCredentials() {
        return Stream.of("123", "1234567", weakDictionaryCredential(), "Pass1!", "short");
    }

    static Stream<String> commonWeakCredentials() {
        return Stream.of("12345678", weakDictionaryCredential(), "admin123", "qwerty123");
    }

    @Test
    @DisplayName("应返回策略描述")
    void shouldReturnPolicyDescription() {
        String description = PasswordValidator.getPolicyDescription();
        assertNotNull(description);
        assertTrue(description.contains("8"));
        assertTrue(description.contains("大写"));
    }
}
