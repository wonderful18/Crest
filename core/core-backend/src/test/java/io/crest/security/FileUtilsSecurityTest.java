package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveUnderDirectoryAllowsSafeFileName() throws Exception {
        File file = FileUtils.resolveUnderDirectory(tempDir.toString(), "safe.xlsx");

        assertTrue(file.toPath().startsWith(tempDir.toRealPath()));
        assertEquals("safe.xlsx", file.getName());
    }

    @Test
    void resolveUnderDirectoryRejectsTraversalFileName() {
        assertThrows(CrestException.class, () -> FileUtils.resolveUnderDirectory(tempDir.toString(), "../escape.xlsx"));
        assertThrows(CrestException.class, () -> FileUtils.resolveUnderDirectory(tempDir.toString(), "a/b.xlsx"));
        assertThrows(CrestException.class, () -> FileUtils.resolveUnderDirectory(tempDir.toString(), "bad\u0000.xlsx"));
    }

    @Test
    void ensureSubDirectoryAllowsSafeDirectoryName() throws Exception {
        File directory = FileUtils.ensureSubDirectory(tempDir.toString(), "1267813530418155520");

        assertTrue(directory.isDirectory());
        assertTrue(directory.toPath().startsWith(tempDir.toRealPath()));
    }

    @Test
    void resolveSubDirectoryDoesNotCreateDirectory() throws Exception {
        File directory = FileUtils.resolveSubDirectory(tempDir.toString(), "missing-task");

        assertFalse(directory.exists());
        assertTrue(directory.toPath().startsWith(tempDir.toRealPath()));
    }

    @Test
    void ensureSubDirectoryRejectsTraversalDirectoryName() {
        assertThrows(CrestException.class, () -> FileUtils.ensureSubDirectory(tempDir.toString(), ".."));
        assertThrows(CrestException.class, () -> FileUtils.ensureSubDirectory(tempDir.toString(), "../escape"));
        assertThrows(CrestException.class, () -> FileUtils.ensureSubDirectory(tempDir.toString(), "nested/escape"));
        assertThrows(CrestException.class, () -> FileUtils.resolveSubDirectory(tempDir.toString(), "../escape"));
    }

    @Test
    void validateExistCreatesCanonicalDirectory() {
        Path directory = tempDir.resolve("reports");

        FileUtils.validateExist(directory.toString());

        assertTrue(Files.isDirectory(directory));
    }

    @Test
    void validateUploadFileSizeRejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", new byte[]{1, 2, 3});

        CrestException exception = assertThrows(CrestException.class,
                () -> FileUtils.validateUploadFileSize(file, 2, "测试文件"));

        assertEquals("测试文件不能超过2B", exception.getMsg());
    }

    @Test
    void validateUploadFileSizeAllowsFileWithinLimit() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", new byte[]{1, 2, 3});

        assertDoesNotThrow(() -> FileUtils.validateUploadFileSize(file, 3, "测试文件"));
    }

    @Test
    void validateContentSizeRejectsOversizedContent() {
        CrestException exception = assertThrows(CrestException.class,
                () -> FileUtils.validateContentSize(new byte[]{1, 2, 3}, 2, "静态资源文件"));

        assertEquals("静态资源文件不能超过2B", exception.getMsg());
    }
}
