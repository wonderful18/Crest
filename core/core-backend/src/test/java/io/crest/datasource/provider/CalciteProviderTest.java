package io.crest.datasource.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CalciteProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void initKeepsDefaultClassLoaderWhenDriverDirectoryIsUnavailable() throws Exception {
        Path notDirectory = Files.createFile(tempDir.resolve("drivers"));
        CalciteProvider provider = new CalciteProvider();
        ReflectionTestUtils.setField(provider, "FILE_PATH", notDirectory.toString());

        provider.init();

        assertNotNull(provider.extendedJdbcClassLoader);
    }
}
