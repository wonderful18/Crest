package io.crest.portal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
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
    void trustedAssetOrderingKeepsDeprecatedResourcesBehindTrustedResources() throws Exception {
        DataPortalServer server = new DataPortalServer();
        Method method = DataPortalServer.class.getDeclaredMethod(
                "orderExpression", DataPortalResourceRequest.class, String.class);
        method.setAccessible(true);

        String expression = (String) method.invoke(server, new DataPortalResourceRequest(), "DESC");

        assertEquals("deprecated ASC, certified DESC, recommended DESC, v.update_time DESC", expression);
    }

    @Test
    void baseQueryBindsUserFiltersAsParameters() throws Exception {
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
