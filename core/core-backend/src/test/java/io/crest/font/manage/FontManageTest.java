package io.crest.font.manage;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FontManageTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadRemovesFileWhenFontValidationFails() {
        FontManage fontManage = new FontManage();
        ReflectionTestUtils.setField(fontManage, "path", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad.ttf",
                "font/ttf",
                "not-a-font".getBytes()
        );

        assertThrows(CrestException.class, () ->
                ReflectionTestUtils.invokeMethod(fontManage, "saveFile", file, "bad-font"));

        assertFalse(Files.exists(tempDir.resolve("bad-font.ttf")));
    }
}
