package io.crest.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkJumpSnapshotContractTest {

    private static final Path REPO = Path.of("").toAbsolutePath().getParent().getParent();

    @Test
    void jumpTargetViewListUsesSnapshotInEditor() throws IOException {
        String api = read("sdk/api/api-base/src/main/java/io/crest/api/visualization/VisualizationLinkJumpApi.java");
        String frontendApi = read("core/core-frontend/src/api/visualization/linkJump.ts");
        String linkJumpSet = read("core/core-frontend/src/components/visualization/LinkJumpSet.vue");
        String service = read("core/core-backend/src/main/java/io/crest/visualization/server/VisualizationLinkJumpService.java");
        String mapper = read("core/core-backend/src/main/resources/mybatis/ExtVisualizationLinkJumpMapper.xml");

        assertTrue(api.contains("/view-table-detail-list/{dvId}/{resourceTable}"));
        assertTrue(frontendApi.contains("viewTableDetailList(dvId, resourceTable = 'snapshot')"));
        assertTrue(linkJumpSet.contains("viewTableDetailList(dvId, 'snapshot')"));
        assertTrue(service.contains("StringUtils.defaultIfBlank(resourceTable, CommonConstants.RESOURCE_TABLE.SNAPSHOT)"));
        assertTrue(mapper.contains("id=\"getViewTableDetailsSnapshot\""));
        assertTrue(mapper.contains("bi_chart_snapshot"));
        assertTrue(mapper.contains("bi_visualization_snapshot"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(REPO.resolve(path));
    }
}
