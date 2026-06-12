package io.crest.asset;

import io.crest.asset.dto.DataAssetProfileRequest;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.log.CrestAudit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataAssetServerTest {

    @Test
    void saveProfileKeepsDataAssetAuditMetadata() throws NoSuchMethodException {
        Method method = DataAssetServer.class.getMethod("saveProfile", DataAssetProfileRequest.class);
        CrestAudit audit = method.getAnnotation(CrestAudit.class);

        assertNotNull(audit);
        assertEquals(LogOT.MODIFY, audit.ot());
        assertEquals(LogST.DATA, audit.st());
        assertEquals("#p0.assetId", audit.id());
    }
}
