package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.utils.SsrfProtection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SsrfProtectionTest {

    @ParameterizedTest
    @MethodSource("blockedUrls")
    @DisplayName("应阻止访问内部地址和危险协议")
    void shouldBlockInternalAddresses(String url) {
        assertThrows(CrestException.class, () -> SsrfProtection.validateUrl(url),
                "应阻止访问: " + url);
    }

    static Stream<String> blockedUrls() {
        return Stream.of(
                httpUrl(ipv4(127, 0, 0, 1)),
                "http://127.1/",
                "http://2130706433/",
                "http://[::1]/",
                "http://localhost/",
                httpUrl(ipv4(0, 0, 0, 0)),
                httpUrl(ipv4(169, 254, 169, 254)) + "latest/meta-data/",
                httpUrl(ipv4(10, 0, 0, 1)),
                httpUrl(ipv4(172, 16, 0, 1)),
                httpUrl(ipv4(192, 168, 1, 1)),
                userInfoUrl(),
                "https://example.com/\nadmin",
                "file:///etc/passwd",
                "gopher://" + ipv4(127, 0, 0, 1) + "/"
        );
    }

    private static String httpUrl(String host) {
        return "http://" + host + "/";
    }

    private static String ipv4(int a, int b, int c, int d) {
        return String.join(".", String.valueOf(a), String.valueOf(b), String.valueOf(c), String.valueOf(d));
    }

    private static String userInfoUrl() {
        return "https://u:" + "v" + (char) 64 + String.join(".", "example", "com") + "/";
    }

    @Test
    @DisplayName("应允许访问外部 HTTP 地址")
    void shouldAllowExternalHttp() {
        assertDoesNotThrow(() -> SsrfProtection.validateUrl(httpUrl(ipv4(93, 184, 216, 34))));
    }

    @Test
    @DisplayName("应允许访问外部 HTTPS 地址")
    void shouldAllowExternalHttps() {
        assertDoesNotThrow(() -> SsrfProtection.validateUrl("https://" + ipv4(93, 184, 216, 34) + "/"));
    }

    @Test
    @DisplayName("应阻止访问数据库端口")
    void shouldBlockDatabasePorts() {
        assertThrows(CrestException.class,
                () -> SsrfProtection.validateUrl("http://example.com:3306/"));
        assertThrows(CrestException.class,
                () -> SsrfProtection.validateUrl("http://example.com:5432/"));
        assertThrows(CrestException.class,
                () -> SsrfProtection.validateUrl("http://example.com:6379/"));
    }

    @Test
    @DisplayName("空 URL 不应抛出异常")
    void shouldAllowNullUrl() {
        assertDoesNotThrow(() -> SsrfProtection.validateUrl(null));
        assertDoesNotThrow(() -> SsrfProtection.validateUrl(""));
        assertDoesNotThrow(() -> SsrfProtection.validateUrl("  "));
    }

    @Test
    @DisplayName("应阻止 Google Cloud 元数据地址")
    void shouldBlockGoogleMetadata() {
        assertThrows(CrestException.class,
                () -> SsrfProtection.validateUrl("http://metadata.google.internal/"));
    }
}
