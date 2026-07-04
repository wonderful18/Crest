package io.crest.system.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SysParameterServerI18nTest {

    @TempDir
    Path tempDir;

    @Test
    void i18nOptionsShouldReturnEmptyMapWhenCustomDirectoryIsMissing() {
        SysParameterServer server = new SysParameterServer();
        ReflectionTestUtils.setField(server, "i18nFrontPath", tempDir.resolve("missing").toString());

        assertEquals(Map.of(), server.i18nOptions());
    }

    @Test
    void i18nOptionsShouldReadValidCustomFrontLanguageFiles() throws Exception {
        Path frontDir = tempDir.resolve("front");
        Files.createDirectories(frontDir);
        Files.writeString(frontDir.resolve("custom_en_US_front_English.js"), "export default {}");
        Files.writeString(frontDir.resolve("readme.txt"), "ignored");
        Files.createDirectories(frontDir.resolve("custom_zh_CN_front_Chinese.js"));

        SysParameterServer server = new SysParameterServer();
        ReflectionTestUtils.setField(server, "i18nFrontPath", "file:" + frontDir);

        assertEquals(Map.of("en-US", "English"), server.i18nOptions());
    }
}
