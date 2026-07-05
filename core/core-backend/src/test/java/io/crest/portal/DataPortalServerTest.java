package io.crest.portal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class DataPortalServerTest {

    @Test
    void trustedAssetOrderingKeepsDeprecatedResourcesBehindTrustedResources() throws ReflectiveOperationException {
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

    @Test
    void baseQueryBindsUserFiltersAsParameters() throws ReflectiveOperationException {
        DataPortalServer server = new DataPortalServer();
        DataPortalPermissionManage permissionManage = mock(DataPortalPermissionManage.class);
        doAnswer(invocation -> null)
                .when(permissionManage)
                .appendReadScope(any(StringBuilder.class), anyList(), eq(7L));
        ReflectionTestUtils.setField(server, "dataPortalPermissionManage", permissionManage);

        String keyword = "sales%' OR 1=1 --";
        DataPortalResourceRequest request = new DataPortalResourceRequest();
        request.setType("screen");
        request.setKeyword(keyword);
        Method normalizeRequest = DataPortalServer.class.getDeclaredMethod(
                "normalizeRequest", DataPortalResourceRequest.class);
        normalizeRequest.setAccessible(true);
        DataPortalResourceRequest normalized = (DataPortalResourceRequest) normalizeRequest.invoke(server, request);

        Method buildBaseQuery = DataPortalServer.class.getDeclaredMethod(
                "buildBaseQuery", Long.class, DataPortalResourceRequest.class);
        buildBaseQuery.setAccessible(true);
        Object queryParts = buildBaseQuery.invoke(server, 7L, normalized);
        String fromWhere = (String) ReflectionTestUtils.getField(queryParts, "fromWhere");
        List<?> args = (List<?>) ReflectionTestUtils.getField(queryParts, "args");

        assertTrue(fromWhere.contains("AND v.type = ?"));
        assertTrue(fromWhere.contains("LOWER(v.name) LIKE LOWER(CONCAT('%', ?, '%'))"));
        assertFalse(fromWhere.contains(keyword));
        assertTrue(args.contains("dataV"));
        assertTrue(args.contains(keyword));
    }
}
