package io.crest.security;

import io.crest.auth.config.SubstituleLoginConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubstituleLoginConfigTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetConfig() {
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "ready", false);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "pwd", null);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "tokenSecret", null);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "tokenSecretExternal", false);
    }

    @Test
    @DisplayName("未显式配置时不应写入固定初始口令")
    void shouldGeneratePasswordWhenNoInitialPasswordConfigured() throws Exception {
        SubstituleLoginConfig config = new SubstituleLoginConfig();
        ReflectionTestUtils.setField(config, "jsonFilePath", tempDir.resolve("substitule.json").toString());

        Map<String, Object> data = config.substituleLoginData(null);
        String pwd = String.valueOf(data.get("pwd"));
        String tokenSecret = String.valueOf(data.get("tokenSecret"));

        assertTrue(StringUtils.isNotBlank(pwd));
        assertTrue(StringUtils.isNotBlank(tokenSecret));
        assertNotEquals("admin", pwd);
    }
}
