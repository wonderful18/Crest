package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
