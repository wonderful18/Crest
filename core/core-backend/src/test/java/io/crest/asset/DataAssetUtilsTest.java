package io.crest.asset;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataAssetUtilsTest {

    @Test
    void normalizePageUsesSafeDefaultsAndCapsPageSize() {
        assertEquals(1, DataAssetUtils.normalizePage(null));
        assertEquals(1, DataAssetUtils.normalizePage(0));
        assertEquals(3, DataAssetUtils.normalizePage(3));

        assertEquals(DataAssetUtils.DEFAULT_PAGE_SIZE, DataAssetUtils.normalizePageSize(null));
        assertEquals(DataAssetUtils.DEFAULT_PAGE_SIZE, DataAssetUtils.normalizePageSize(0));
        assertEquals(50, DataAssetUtils.normalizePageSize(50));
        assertEquals(DataAssetUtils.MAX_PAGE_SIZE, DataAssetUtils.normalizePageSize(500));
    }

    @Test
    void normalizeAssetTypeAcceptsRouteAliasesAndRejectsUnknownTypes() {
        assertNull(DataAssetUtils.normalizeAssetType(null));
        assertNull(DataAssetUtils.normalizeAssetType("all"));
        assertEquals(DataAssetUtils.TYPE_SCREEN, DataAssetUtils.normalizeAssetType("dataV"));
        assertEquals(DataAssetUtils.TYPE_PANEL, DataAssetUtils.normalizeAssetType("dashboard"));
        assertEquals(DataAssetUtils.TYPE_DATASET, DataAssetUtils.normalizeAssetType(" dataset "));

        assertThrows(CrestException.class, () -> DataAssetUtils.normalizeAssetType("unknown"));
    }

    @Test
    void resourceTypeOnlyMapsAssetsWithBusinessResourcePermissions() {
        assertEquals(DataAssetUtils.TYPE_DATASOURCE,
                DataAssetUtils.resourceTypeForAsset(DataAssetUtils.TYPE_DATASOURCE));
        assertEquals(DataAssetUtils.TYPE_DATASET,
                DataAssetUtils.resourceTypeForAsset(DataAssetUtils.TYPE_DATASET));
        assertEquals(DataAssetUtils.TYPE_PANEL,
                DataAssetUtils.resourceTypeForAsset(DataAssetUtils.TYPE_PANEL));
        assertNull(DataAssetUtils.resourceTypeForAsset(DataAssetUtils.TYPE_CHART));
        assertNull(DataAssetUtils.resourceTypeForAsset(DataAssetUtils.TYPE_SHARE));
    }

    @Test
    void deprecatedGovernanceStatusClearsTrustFlags() {
        DataAssetUtils.GovernanceStatus status = DataAssetUtils.normalizeGovernanceStatus(true, true, true);

        assertEquals(false, status.certified());
        assertEquals(false, status.recommended());
        assertEquals(true, status.deprecated());
    }
}
