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
import java.util.stream.Stream;

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
    void frontendSourcesShouldNotUseKnownBulkRenamedRoutes() throws IOException {
        assertFrontendTreeDoesNotContain(List.of(
                "/chart/save",
                "/dataset-tree/save",
                "/dataset-sync/save",
                "/dataset/row-permissions/save",
                "/dataset/column-permissions/save",
                "/dataset-field/save",
                "/datasource/excel-data/save",
                "/datasource/save",
                "/sync/datasource/save",
                "/data-visualization/record",
                "/data-visualization/save",
                "/watermark/save",
                "/sys-parameter/basic/save",
                "/engine/save"
        ));
    }

    @Test
    void frontendSourcesShouldNotUseKnownEditToChangeMisreplacements() throws IOException {
        assertFrontendTreeDoesNotContain(List.of(
                "el-icon-change",
                "main-change",
                "panel_change",
                "show-position=\"change\"",
                "show-position='change'",
                "de-dashboard-editor-back"
        ));
    }

    @Test
    void datasetStructurePreviewShouldNotShadowFieldTypeLookup() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/visualized/data/dataset/index.vue",
                List.of("cellData: fieldTypeCode", "fieldType[fieldTypeCode]"),
                List.of("cellData: fieldType })", "fieldType[fieldType]"));
    }

    @Test
    void datasetCreateRouteShouldNotSendUndefinedPid() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/visualized/data/dataset/index.vue",
                List.of("const query = data?.id ? { pid: data.id } : {}", "query"),
                List.of("pid: data?.id"));
    }

    @Test
    void resourceTreeDefaultSortShouldGuardMissingCacheValue() {
        List<String> resourceTreeFiles = List.of(
                "core/core-frontend/src/views/visualized/data/dataset/index.vue",
                "core/core-frontend/src/views/visualized/data/datasource/index.vue",
                "core/core-frontend/src/views/common/CrestResourceTree.vue",
                "core/core-frontend/src/views/visualized/data/dataset/form/CreatDsGroup.vue",
                "core/core-frontend/src/views/visualized/data/datasource/form/CreatDsGroup.vue"
        );

        resourceTreeFiles.forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of("const getDefaultSortType = () =>", "Number.isInteger(sortIndex)"),
                        List.of("sortList[Number(wsCache.get('TreeSort-backend')) ?? 1]"));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
    }

    @Test
    void datasetCopyRouteShouldUseQueryParam() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/visualized/data/dataset/index.vue",
                List.of("query: { copyId: data.id }"),
                List.of("params: {\n        id: data.id"));
        assertFrontendContract("core/core-frontend/src/views/visualized/data/dataset/form/index.vue",
                List.of("let { id, datasourceId, tableName, copyId } = route.query", "legacyCopyId"),
                List.of("let { id: copyId } = route.params"));
    }

    @Test
    void workbranchResourceShortcutsShouldUseQueryParam() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/workbranch/ShortcutTable.vue",
                List.of("query: { id: sourceId }"),
                List.of("params: {\n          id: sourceId"));
    }

    @Test
    void queryCascadeFiltersShouldNotSpreadScalarValues() throws IOException {
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Select.vue",
                List.of("normalizeCascadeValue(item.selectValue)", "normalizeCascadeValue(item.currentSelectValue)"),
                List.of("value: [...item.selectValue]", "value: [...item.currentSelectValue]"));
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Tree.vue",
                List.of("normalizeCascadeValue(item.selectValue)", "normalizeCascadeValue(item.currentSelectValue)"),
                List.of("value: [...item.selectValue]", "value: [...item.currentSelectValue]"));
    }

    @Test
    void querySelectCustomSortShouldUseStableComparator() throws IOException {
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Select.vue",
                List.of(
                        "const sortList = config.value.sortList",
                        "const aIndex = sortList.indexOf(a.value)",
                        "const bIndex = sortList.indexOf(b.value)"
                ),
                List.of(".sort(a => {"));
    }

    @Test
    void mobileQuerySelectShouldNormalizePopupSelectValue() throws IOException {
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/VanPopupSelect.vue",
                List.of("const toSelectValueList = value =>", "checkList.value = toSelectValueList(props.selectValue)"),
                List.of("checkList.value = [...props.selectValue]"));
    }

    @Test
    void frontendSourcesShouldNotUseDefaultClassBulkRenameArtifacts() throws IOException {
        assertFrontendTreeDoesNotContain(List.of("crestfault"));
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/FilterTime.vue",
                List.of("?.label ||\n    intervalTypeList[0].label", ".ed-checkbox.ed-checkbox--default"),
                List.of(".ed-checkbox.ed-checkbox--crestfault"));
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/RangeFilterTime.vue",
                List.of("?.label ||\n    intervalTypeList[0].label", ".ed-checkbox.ed-checkbox--default"),
                List.of(".ed-checkbox.ed-checkbox--crestfault"));
        assertFrontendContract("core/core-frontend/src/views/login/index.vue",
                List.of(".ed-form-item--default"),
                List.of(".ed-form-item--crestfault"));
    }

    @Test
    void chartFilterDynamicTimeShouldGuardUnknownPresetValue() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/filter/auth-tree-chart/FilterFiled.vue",
                List.of("?.label", "presetTimeLabel || relativeToCurrent || ''"),
                List.of("].find(ele => ele.value === relativeToCurrent).label"));
    }

    @Test
    void frontendOptionLabelsShouldGuardUnknownValues() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/system/parameter/basic/BasicInfo.vue",
                List.of("pvpOptions.find(cur => cur.value === selectedPvp.value)?.label || pvpOptions[0].label"),
                List.of("pvpOptions.filter(cur => cur.value === selectedPvp.value)[0].label"));
        assertFrontendContract("core/core-frontend/src/views/system/common/InfoTemplate.vue",
                List.of("options.find(item => item.value === val)?.label ?? options[0].label"),
                List.of("options.filter(item => item.value === val)[0].label"));
    }

    @Test
    void queryConfigurationShouldGuardIncompleteParameterState() throws IOException {
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/QueryConditionConfiguration.vue",
                List.of(
                        "const timeParameter = curComponent.value.parameters?.find(",
                        "const timeRangeParameter = curComponent.value.parametersArr[componentId][0]",
                        "if (!timeRangeParameter?.type?.length) return",
                        "if (!timeParameter?.type?.length) return timeList",
                        "if (!field?.id || !datasetId) return pre",
                        "const fieldList = datasetMap[datasetId]?.fields?.dimensionList || next.dataset?.fields || []"
                ),
                List.of(
                        "const [v1, v2] = curComponent.value.parametersArr[componentId][0].type",
                        "curComponent.value.parameters?.filter(\n    ele => ele.fieldType === 1 && !!ele.variableName\n  )[0].type",
                        "next.treeFieldList[0].id : next.field.id",
                        "datasetMap[next.dataset.id]?.fields?.dimensionList || next.dataset.fields"
                ));
    }

    @Test
    void frontendInteractiveStateShouldGuardMissingSelections() throws IOException {
        assertFrontendContract("core/core-frontend/src/store/modules/data-visualization/compose.ts",
                List.of(
                        "const sourceGroupComponent = componentData.value.find",
                        "if (!sourceGroupComponent?.propValue) return",
                        "const components = curComponent.value.propValue || []",
                        "const pComponentTarget = (pComponent?.propValue || []).filter",
                        "if (areaDataValue.components.length <= 1)"
                ),
                List.of(
                        "componentData.value.filter(ele => ele.id === groupId)[0]",
                        "const components = curComponent.value.propValue\n",
                        "pComponent.propValue.filter",
                        "areaDataValue.components <= 1"
                ));
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/dataset-select/DatasetSelect.vue",
                List.of("datasetTree.value[0]?.id === '0'", "return datasetTree.value[0].children || []", "function flatTree(tree: Tree[] = [])"),
                List.of("datasetTree.value[0].id === '0'", "return datasetTree.value[0].children\n", "function flatTree(tree: Tree[])"));
    }

    @Test
    void chartInteractionStateShouldGuardMissingDrillAndQuotaData() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/chart/components/views/components/ChartComponentG2Plot.vue",
                List.of("if (curFiled?.id)", "let quotaList = state.pointParam.data.quotaList || []", "} else if (quotaList.length)"),
                List.of("if (curView.drill) {\n      const curFiled = curView.drillFields[curView.drillFilters.length]\n      fieldIds.push(curFiled.id)",
                        "let quotaList = state.pointParam.data.quotaList\n",
                        "} else {\n    quotaList[0]['value']"));
        assertFrontendContract("core/core-frontend/src/views/chart/components/views/components/ChartComponentS2.vue",
                List.of("view.value.drillFields[drillLength.value]?.id === itemId"),
                List.of("view.value.drillFields[drillLength.value].id === itemId"));
        assertFrontendContract("core/core-frontend/src/views/chart/components/views/index.vue",
                List.of("if (!props.view.drillFields?.length)"),
                List.of());
    }

    @Test
    void visualizationUserLookupShouldUseCurrentUserTable() throws IOException {
        assertFrontendContract("core/core-backend/src/main/resources/mybatis/ExtDataVisualizationMapper.xml",
                List.of("from iam_user where id"),
                List.of("from per_user"));
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

    private void assertFrontendTreeDoesNotContain(List<String> forbiddenTokens) throws IOException {
        Path frontendRoot = repoRoot().resolve("core/core-frontend/src");
        try (Stream<Path> paths = Files.walk(frontendRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/node_modules/"))
                    .filter(this::isFrontendSourceFile)
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            forbiddenTokens.forEach(token -> assertFalse(source.contains(token),
                                    frontendRoot.relativize(path) + " should not contain " + token));
                        } catch (IOException e) {
                            throw new AssertionError("Unable to read " + path, e);
                        }
                    });
        }
    }

    private boolean isFrontendSourceFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".vue")
                || name.endsWith(".ts")
                || name.endsWith(".tsx")
                || name.endsWith(".js")
                || name.endsWith(".jsx")
                || name.endsWith(".mjs")
                || name.endsWith(".cjs")
                || name.endsWith(".json")
                || name.endsWith(".css")
                || name.endsWith(".less")
                || name.endsWith(".scss")
                || name.endsWith(".sass")
                || name.endsWith(".html")
                || name.endsWith(".md")
                || name.endsWith(".svg")
                || name.endsWith(".yaml")
                || name.endsWith(".yml");
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
