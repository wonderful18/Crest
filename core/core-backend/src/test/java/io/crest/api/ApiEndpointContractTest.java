package io.crest.api;

import io.crest.api.dataset.dto.EnumValueRequest;
import io.crest.api.dataset.dto.MultFieldValuesRequest;
import io.crest.constant.SystemSettingConstants;
import io.crest.utils.SystemSettingUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiEndpointContractTest {

    @Test
    void accountSecuritySettingsShouldBePersistable() {
        List.of(
                SystemSettingConstants.INITIAL_PASSWORD,
                "basic.pwdStrategy",
                SystemSettingConstants.DIP,
                SystemSettingConstants.PVP,
                SystemSettingConstants.LOGIN_LIMIT,
                SystemSettingConstants.LOGIN_LIMIT_RATE,
                SystemSettingConstants.LOGIN_LIMIT_TIME
        ).forEach(key -> assertFalse(SystemSettingUtils.restrictedSetting(key)));
    }

    @Test
    void portalPreviewQueryFiltersShouldCarryVisualizationContext() throws NoSuchFieldException {
        assertNotNull(EnumValueRequest.class.getDeclaredField("visualizationId"));
        assertNotNull(MultFieldValuesRequest.class.getDeclaredField("visualizationId"));
    }

    @Test
    void dataPortalScopeShouldUseNumericResourceComparison() throws IOException {
        String source = readSource("core/core-backend/src/main/java/io/crest/portal/DataPortalPermissionManage.java");

        assertTrue(source.contains("numberCast(\"p.resource_id\")"));
        assertFalse(source.contains("p.resource_id = CAST(v.id AS CHAR)"));
    }

    private String readSource(String path) throws IOException {
        Path sourcePath = Path.of(path);
        if (!Files.exists(sourcePath)) {
            sourcePath = Path.of("..", "..", path);
        }
        return Files.readString(sourcePath);
    }
}
