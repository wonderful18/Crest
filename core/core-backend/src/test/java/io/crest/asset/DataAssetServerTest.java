package io.crest.asset;

import io.crest.asset.dto.DataAssetProfileRequest;
import io.crest.asset.dto.DataAssetRequest;
import io.crest.auth.CrestApiPath;
import io.crest.auth.CrestPermit;
import io.crest.constant.AuthResourceEnum;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.log.CrestAudit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataAssetServerTest {

    @Test
    void dataAssetServerDeclaresMenuPermissionScope() {
        CrestApiPath apiPath = DataAssetServer.class.getAnnotation(CrestApiPath.class);

        assertNotNull(apiPath);
        assertArrayEquals(new String[]{"/data-assets"}, apiPath.value());
        assertEquals(AuthResourceEnum.DATA_ASSET, apiPath.rt());
    }

    @Test
    void readEndpointsRequireDataAssetMenuReadPermission() throws NoSuchMethodException {
        assertPermit(DataAssetServer.class.getMethod("page", Integer.class, Integer.class,
                DataAssetRequest.class), "m:read");
        assertPermit(DataAssetServer.class.getMethod("detail", String.class, String.class), "m:read");
        assertPermit(DataAssetServer.class.getMethod("impact", String.class, String.class), "m:read");
        assertPermit(DataAssetServer.class.getMethod("owners"), "m:read");
    }

    @Test
    void saveProfileKeepsDataAssetAuditMetadata() throws NoSuchMethodException {
        Method method = DataAssetServer.class.getMethod("saveProfile", DataAssetProfileRequest.class);
        CrestAudit audit = method.getAnnotation(CrestAudit.class);
        assertPermit(method, "m:read");

        assertNotNull(audit);
        assertEquals(LogOT.MODIFY, audit.ot());
        assertEquals(LogST.DATA, audit.st());
        assertEquals("#p0.assetId", audit.id());
    }

    private static void assertPermit(Method method, String expected) {
        CrestPermit permit = method.getAnnotation(CrestPermit.class);
        assertNotNull(permit);
        assertArrayEquals(new String[]{expected}, permit.value());
    }
}
