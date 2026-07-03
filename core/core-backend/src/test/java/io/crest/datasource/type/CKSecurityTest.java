package io.crest.datasource.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CKSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void sslCertificateContentUsesStableSingleFile() throws Exception {
        String originalTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.toString());
        try {
            CK ck = new CK();
            ck.setHost("clickhouse.example.com");
            ck.setPort(8123);
            ck.setDataBase("default");
            ck.setSslKey("-----BEGIN PRIVATE KEY-----\nunit-test-key-" + tempDir.getFileName() + "\n-----END PRIVATE KEY-----");

            String firstJdbc = ck.getJdbc();
            String secondJdbc = ck.getJdbc();

            assertEquals(firstJdbc, secondJdbc);
            assertTrue(firstJdbc.contains("ssl=true"));
            assertTrue(firstJdbc.contains("sslKey="));

            Path certDir = tempDir.resolve("crest").resolve("clickhouse-ssl");
            try (Stream<Path> files = Files.list(certDir)) {
                assertEquals(1L, files.filter(Files::isRegularFile).count());
            }
        } finally {
            if (originalTmpDir == null) {
                System.clearProperty("java.io.tmpdir");
            } else {
                System.setProperty("java.io.tmpdir", originalTmpDir);
            }
        }
    }
}
