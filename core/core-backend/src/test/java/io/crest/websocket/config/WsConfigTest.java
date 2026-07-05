package io.crest.websocket.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WsConfigTest {

    @Test
    void allowedOriginsUsesConfiguredOriginListWithoutWildcard() {
        WsConfig config = new WsConfig();
        ReflectionTestUtils.setField(config, "originList", List.of(
                " https://crest.example.com ",
                "",
                "https://ops.example.com",
                "https://crest.example.com"
        ));

        assertArrayEquals(
                new String[]{"https://crest.example.com", "https://ops.example.com"},
                config.allowedOrigins()
        );
    }

    @Test
    void allowedOriginsReturnsEmptyArrayWhenNotConfigured() {
        WsConfig config = new WsConfig();
        ReflectionTestUtils.setField(config, "originList", List.of("", "  "));

        assertArrayEquals(new String[0], config.allowedOrigins());
    }
}
