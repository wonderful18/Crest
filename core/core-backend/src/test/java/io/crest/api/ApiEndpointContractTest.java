package io.crest.api;

import io.crest.api.chart.ChartViewApi;
import io.crest.api.dataset.DatasetSyncApi;
import io.crest.api.dataset.DatasetTableApi;
import io.crest.api.dataset.DatasetTableSqlLogApi;
import io.crest.api.dataset.DatasetTreeApi;
import io.crest.api.ds.DatasourceApi;
import io.crest.api.ds.EngineApi;
import io.crest.api.permissions.dataset.api.ColumnPermissionsApi;
import io.crest.api.permissions.dataset.api.RowPermissionsApi;
import io.crest.api.sync.datasource.api.SyncDatasourceApi;
import io.crest.api.system.SysParameterApi;
import io.crest.api.visualization.DataVisualizationApi;
import io.crest.api.visualization.VisualizationWatermarkApi;
import io.crest.embedded.server.EmbeddedTokenServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiEndpointContractTest {

    @Test
    void saveEndpointsShouldKeepRecordMappings() {
        List<Endpoint> endpoints = List.of(
                new Endpoint(ChartViewApi.class, "save", "record"),
                new Endpoint(DatasetTreeApi.class, "save", "record"),
                new Endpoint(DatasetSyncApi.class, "save", "record"),
                new Endpoint(DatasetTableApi.class, "save", "record"),
                new Endpoint(DatasetTableSqlLogApi.class, "save", "record"),
                new Endpoint(DatasourceApi.class, "save", "/record"),
                new Endpoint(DatasourceApi.class, "saveExcelData", "/excel-data/record"),
                new Endpoint(SyncDatasourceApi.class, "save", "/record"),
                new Endpoint(EngineApi.class, "save", "/record"),
                new Endpoint(SysParameterApi.class, "saveBasicSetting", "/basic/record"),
                new Endpoint(VisualizationWatermarkApi.class, "saveWatermarkInfo", "/record"),
                new Endpoint(RowPermissionsApi.class, "save", "record"),
                new Endpoint(ColumnPermissionsApi.class, "save", "record")
        );

        endpoints.forEach(endpoint -> assertPostMapping(endpoint.api(), endpoint.method(), endpoint.path()));
    }

    @Test
    void frontendSaveUrlsShouldMatchRecordMappings() throws IOException {
        assertFrontendContract("core/core-frontend/src/api/chart.ts",
                List.of("/chart/record"), List.of("/chart/save"));
        assertFrontendContract("core/core-frontend/src/api/dataset.ts",
                List.of(
                        "/dataset-tree/record",
                        "/dataset-sync/record",
                        "/dataset/row-permissions/record",
                        "/dataset/column-permissions/record",
                        "/dataset-field/record",
                        "export const exportTasks = (page = 1, limit = 10, status = 'ALL')"
                ),
                List.of(
                        "/dataset-tree/save",
                        "/dataset-sync/save",
                        "/dataset/row-permissions/save",
                        "/dataset/column-permissions/save",
                        "/dataset-field/save",
                        "/export-center/export-tasks/${undefined}"
                ));
        assertFrontendContract("core/core-frontend/src/api/datasource.ts",
                List.of("/datasource/excel-data/record", "/datasource/record"),
                List.of("/datasource/excel-data/save", "/datasource/save"));
        assertFrontendContract("core/core-frontend/src/api/sync/syncDatasource.ts",
                List.of("/sync/datasource/record"), List.of("/sync/datasource/save"));
        assertFrontendContract("core/core-frontend/src/api/visualization/dataVisualization.ts",
                List.of("/data-visualization/canvas"), List.of("/data-visualization/record", "/data-visualization/save"));
        assertFrontendContract("core/core-frontend/src/api/watermark.ts",
                List.of("/watermark/record"), List.of("/watermark/save"));
        assertFrontendContract("core/core-frontend/src/views/system/parameter/basic/BasicEdit.vue",
                List.of("/sys-parameter/basic/record"), List.of("/sys-parameter/basic/save"));
        assertFrontendContract("core/core-frontend/src/views/system/parameter/engine/EngineEdit.vue",
                List.of("/engine/record"), List.of("/engine/save"));
        assertFrontendContract("core/core-frontend/src/views/system/site/index.vue",
                List.of("/sys-parameter/basic/record"), List.of("/sys-parameter/basic/save"));
    }

    @Test
    void dataVisualizationCreateShouldUseCanvasMapping() {
        assertPostMapping(DataVisualizationApi.class, "saveCanvas", "/canvas");
    }

    @Test
    void embeddedTokenArgsShouldKeepCurrentContract() throws IOException {
        assertGetMapping(EmbeddedTokenServer.class, "getTokenArgs", "/token-args");
        assertGetMapping(EmbeddedTokenServer.class, "getTokenArgs", "/getTokenArgs");
        assertFrontendContract("core/core-frontend/src/config/axios/service.ts",
                List.of("X-CREST-EMBEDDED-TOKEN"), List.of("X-EMBEDDED-TOKEN"));
        assertFrontendContract("core/core-frontend/src/pages/panel/DashboardPreview.vue",
                List.of("/embedded/token-args"), List.of("/embedded/getTokenArgs"));
        assertFrontendContract("core/core-frontend/src/pages/panel/ViewWrapper.vue",
                List.of("/embedded/token-args"), List.of("/embedded/getTokenArgs"));
    }

    private void assertPostMapping(Class<?> api, String methodName, String expectedPath) {
        Method method = Arrays.stream(api.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(api.getName() + "#" + methodName + " missing"));
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping, api.getName() + "#" + methodName + " should be a POST endpoint");

        String[] paths = mapping.value().length > 0 ? mapping.value() : mapping.path();
        assertTrue(Arrays.asList(paths).contains(expectedPath),
                api.getSimpleName() + "#" + methodName + " should map to " + expectedPath);
    }

    private void assertGetMapping(Class<?> api, String methodName, String expectedPath) {
        Method method = Arrays.stream(api.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(api.getName() + "#" + methodName + " missing"));
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping, api.getName() + "#" + methodName + " should be a GET endpoint");

        String[] paths = mapping.value().length > 0 ? mapping.value() : mapping.path();
        assertTrue(Arrays.asList(paths).contains(expectedPath),
                api.getSimpleName() + "#" + methodName + " should map to " + expectedPath);
    }

    private void assertFrontendContract(
            String relativePath,
            List<String> requiredTokens,
            List<String> forbiddenTokens
    ) throws IOException {
        String source = Files.readString(repoRoot().resolve(relativePath));
        requiredTokens.forEach(token -> assertTrue(source.contains(token),
                relativePath + " should contain " + token));
        forbiddenTokens.forEach(token -> assertFalse(source.contains(token),
                relativePath + " should not contain " + token));
    }

    private Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("core/core-frontend/src/api/dataset.ts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate Crest repository root");
    }

    private record Endpoint(Class<?> api, String method, String path) {
    }
}
