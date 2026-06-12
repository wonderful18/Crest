package io.crest.portal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPortalServerTest {

    @Test
    void trustedAssetOrderingKeepsDeprecatedResourcesBehindTrustedResources() throws Exception {
        DataPortalServer server = new DataPortalServer();
        Method method = DataPortalServer.class.getDeclaredMethod(
                "orderExpression", DataPortalResourceRequest.class, String.class);
        method.setAccessible(true);

        String expression = (String) method.invoke(server, new DataPortalResourceRequest(), "DESC");

        assertEquals("deprecated ASC, certified DESC, recommended DESC, v.update_time DESC", expression);
    }

    @Test
    void trustSignalsIgnoreDeprecatedDatasets() throws IOException {
        Path sourcePath = Path.of("src/main/java/io/crest/portal/DataPortalServer.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("core/core-backend/src/main/java/io/crest/portal/DataPortalServer.java");
        }
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("AND p.certified = 1\n                             AND p.deprecated = 0"));
        assertTrue(source.contains("AND p.recommended = 1\n                             AND p.deprecated = 0"));
        assertFalse(source.contains(
                "return \"certified DESC, recommended DESC, deprecated ASC, v.update_time \" + direction;"));
    }
}
