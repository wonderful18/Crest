package io.crest.auth.interceptor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CorsConfigTest {

    @Test
    void normalizedOriginsTrimsDeduplicatesAndDropsBlankValues() {
        CorsConfig config = new CorsConfig();

        assertArrayEquals(
                new String[]{"https://crest.example.com", "https://ops.example.com"},
                config.normalizedOrigins(List.of(" https://crest.example.com ", "", "https://ops.example.com", "https://crest.example.com"))
        );
    }

    @Test
    void normalizedOriginsReturnsEmptyArrayWhenNotConfigured() {
        CorsConfig config = new CorsConfig();

        assertArrayEquals(new String[0], config.normalizedOrigins(List.of("", " ")));
    }
}
