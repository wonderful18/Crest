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
import io.crest.constant.SystemSettingConstants;
import io.crest.embedded.server.EmbeddedTokenServer;
import io.crest.utils.SystemSettingUtils;
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
    void userPasswordManagementShouldShowDefaultPassword() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/system/user/index.vue",
                List.of(
                        "/user/default-password",
                        "/user/reset-password/${row.id}",
                        "默认密码：",
                        "复制密码",
                        "新建用户成功",
                        "密码已重置"
                ),
                List.of(
                        "密码已重置')"
                ));
        assertFrontendContract("core/core-frontend/src/views/system/parameter/basic/BasicInfo.vue",
                List.of("setting_basic.initialPassword", "账号安全"),
                List.of("AI"));
        assertFrontendContract("core/core-frontend/src/views/system/parameter/basic/BasicEdit.vue",
                List.of("item.pkey === 'initialPassword'", "autocomplete=\"new-password\""),
                List.of("AI"));
        assertFrontendContract("core/core-backend/src/main/java/io/crest/substitute/permissions/user/SubstituteUserServer.java",
                List.of("return crestUserManage.defaultPwd();"),
                List.of("return \"\";"));
    }

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
    void dataPortalShouldRemainTheDefaultUserEntry() throws IOException {
        assertFrontendContract("core/core-frontend/src/router/index.ts",
                List.of("redirect: '/portal'", "path: '/portal'", "DataPortal.vue", "path: '/portal/view/:id'", "DataPortalView.vue"),
                List.of("redirect: '/workbranch/index'"));
        assertFrontendContract("core/core-frontend/src/permission.ts",
                List.of("const portalHome = '/portal'", "getBackendAccess", "isBackendRoute"),
                List.of("const portalHome = '/workbranch/index'"));
        assertFrontendContract("core/core-frontend/src/views/login/index.vue",
                List.of("'/portal'", "'/workbranch/index'"),
                List.of("let queryRedirectPath = '/workbranch/index'"));
    }

    @Test
    void dataPortalScopeShouldNotUseStringCastForResourceId() throws IOException {
        assertFrontendContract("core/core-backend/src/main/java/io/crest/portal/DataPortalPermissionManage.java",
                List.of(
                        "canReadPublishedVisualization",
                        "canReadPublishedVisualizationDataset",
                        "FROM bi_chart c",
                        "CAST(p.resource_id AS UNSIGNED) = v.id",
                        "v.type = 'dataV' AND p.resource_type = 'screen'",
                        "v.type = 'dashboard' AND p.resource_type = 'panel'"
                ),
                List.of(
                        "p.resource_id = CAST(v.id AS CHAR)",
                        "p.resource_type = CASE"
                ));
        assertFrontendContract("core/core-backend/src/main/java/io/crest/portal/DataPortalServer.java",
                List.of(
                        "\" ORDER BY \" + orderColumn(normalized) + \" \" + orderDirection + \" LIMIT ? OFFSET ?\"",
                        "INNER JOIN workspace_favorite_resource wfr ON wfr.resource_id = v.id",
                        "INNER JOIN workspace_recent_resource wrr ON wrr.resource_id = v.id",
                        "AS favorite",
                        "recordRecent(resource)",
                        "OptConstants.OPT_RESOURCE_TYPE.VISUALIZATION",
                        "\" LIMIT 1\""
                ),
                List.of(
                        "p.resource_id = CAST(v.id AS CHAR)",
                        "p.resource_type = CASE",
                        "ORDER BY v.update_time \"\"\"",
                        "AND v.id = ?LIMIT"
                ));
    }

    @Test
    void dataPortalHomeShouldDefaultToFavorites() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/portal/DataPortal.vue",
                List.of(
                        "const systemTitle = computed(() => appearanceStore.getSiteTitle)",
                        "<span class=\"portal-title\">{{ systemTitle }}</span>",
                        "const activeName = ref(FAVORITES)",
                        "{ title: '我的收藏', name: FAVORITES }",
                        "{ title: '全部资源', name: ALL_RESOURCES }",
                        "const resourceQueryFrom = () => (activeName.value === ALL_RESOURCES ? undefined : activeName.value)",
                        "queryFrom: resourceQueryFrom()",
                        "openAllResources(action)",
                        "storeApi({",
                        "favoriteLoadingId",
                        "row.favorite ? '取消收藏' : '收藏'",
                        "label=\"更新时间\""
                ),
                List.of(
                        "研发效能指标管理系统",
                        "{ title: '我的资源', name: 'store' }",
                        "label=\"创建人\"",
                        "label=\"组件\""
                ));
        assertFrontendContract("core/core-frontend/src/api/dataPortal.ts",
                List.of("queryFrom?: string"),
                List.of());
    }

    @Test
    void portalBrandTitleShouldFollowSiteTitleSetting() throws IOException {
        assertFrontendContract("core/core-frontend/src/store/modules/appearance.ts",
                List.of(
                        "siteTitle: 'Crest'",
                        "getSiteTitle(): string",
                        "async setAppearance(isCrestBi?: boolean, force = false)",
                        "this.siteTitle = data.siteTitle || 'Crest'"
                ),
                List.of());
        assertFrontendContract("core/core-frontend/src/views/portal/DataPortal.vue",
                List.of(
                        "useAppearanceStoreWithOut",
                        "const systemTitle = computed(() => appearanceStore.getSiteTitle)",
                        "<span class=\"portal-title\">{{ systemTitle }}</span>"
                ),
                List.of("研发效能指标管理系统"));
        assertFrontendContract("core/core-frontend/src/views/portal/DataPortalView.vue",
                List.of(
                        "useAppearanceStoreWithOut",
                        "const systemTitle = computed(() => appearanceStore.getSiteTitle)",
                        "<span class=\"portal-title\">{{ systemTitle }}</span>"
                ),
                List.of("研发效能指标管理系统"));
        assertFrontendContract("core/core-frontend/src/views/system/site/index.vue",
                List.of("await appearanceStore.setAppearance(false, true)"),
                List.of("await appearanceStore.setAppearance()\n"));
    }

    @Test
    void portalPreviewQueryFiltersShouldUseVisualizationReadContext() throws IOException {
        assertFrontendContract("sdk/api/api-base/src/main/java/io/crest/api/dataset/dto/EnumValueRequest.java",
                List.of("private Long visualizationId"),
                List.of());
        assertFrontendContract("sdk/api/api-base/src/main/java/io/crest/api/dataset/dto/MultFieldValuesRequest.java",
                List.of("private Long visualizationId"),
                List.of());
        assertFrontendContract("core/core-backend/src/main/java/io/crest/dataset/manage/DatasetGroupManage.java",
                List.of(
                        "canReadPublishedVisualizationDataset(visualizationId, id)",
                        "sqlParamsForVisualizationRead"
                ),
                List.of("canReadPublishedVisualization(visualizationId, null)"));
        assertFrontendContract("core/core-backend/src/main/java/io/crest/dataset/manage/DatasetDataManage.java",
                List.of(
                        "canReadPublishedVisualizationDataset(multFieldValuesRequest.getVisualizationId(), datasetGroupId)",
                        "canReadPublishedVisualizationDataset(request.getVisualizationId(), datasetGroupId)",
                        "datasetGroupInfoDTOForVisualizationRead(",
                        "sqlParamsForVisualizationRead("
                ),
                List.of("DatasetGroupInfoDTO datasetGroupInfoDTO = datasetGroupManage.datasetGroupInfoDTO(datasetGroupId, null);"));
        assertFrontendContract("core/core-frontend/src/api/dataset.ts",
                List.of("visualizationId?: string | number"),
                List.of());
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Select.vue",
                List.of(
                        "const currentVisualizationId = () => dvMainStore.dvInfo?.id || router.currentRoute.value.query.dvId",
                        "visualizationId: currentVisualizationId()"
                ),
                List.of());
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Tree.vue",
                List.of(
                        "const currentVisualizationId = () => dvMainStore.dvInfo?.id || router.currentRoute.value.query.dvId",
                        "visualizationId: currentVisualizationId()"
                ),
                List.of());
    }

    @Test
    void portalPreviewShouldUseReadScopeWithoutBackendTree() throws IOException {
        assertFrontendContract("core/core-backend/src/main/java/io/crest/visualization/server/DataVisualizationServer.java",
                List.of(
                        "DataPortalPermissionManage",
                        "dataPortalPermissionManage.canReadPublishedVisualization(dvId, result.getType())"
                ),
                List.of("DataVisualizationConstants.QUERY_SOURCE.MAIN_EDIT.equals(request.getSource())\n            )"));
        assertFrontendContract("core/core-backend/src/main/java/io/crest/chart/manage/ChartViewManege.java",
                List.of(
                        "DataPortalPermissionManage",
                        "dataPortalPermissionManage.canReadPublishedVisualization(sceneId, null)",
                        "portalVisualizationRead",
                        "requireChartReadAccess(chart.getCreateBy(), chart.getSceneId(), chart.getTableId())"
                ),
                List.of());
        assertFrontendContract("core/core-backend/src/main/java/io/crest/chart/manage/ChartDataManage.java",
                List.of(
                        "DataPortalPermissionManage",
                        "datasetGroupInfoDTOForVisualizationRead",
                        "sqlParamsForVisualizationRead",
                        "portalVisualizationRead || corePermissionManage.checkAuth(dto)"
                ),
                List.of("datasetGroupManage.datasetGroupInfoDTO(view.getTableId(), null)"));
        assertFrontendContract("core/core-backend/src/main/java/io/crest/dataset/manage/DatasetGroupManage.java",
                List.of(
                        "datasetGroupInfoDTOForVisualizationRead",
                        "sqlParamsForVisualizationRead",
                        "dataPortalPermissionManage.canReadPublishedVisualizationDataset(visualizationId, id)"
                ),
                List.of());
        assertFrontendContract("core/core-frontend/src/permission.ts",
                List.of("!userStore.getBackendAccess && isReadOnlyRoute(to.path, to.name)"),
                List.of());
        assertFrontendContract("core/core-frontend/src/store/modules/interactive.ts",
                List.of("const treeNodes = Array.isArray(list)", "treeNodes[0]?.weight"),
                List.of("list[0]['weight']"));
    }

    @Test
    void standaloneRowPermissionShouldTreatMissingApiAsNoRules() throws IOException {
        assertFrontendContract("core/core-backend/src/main/java/io/crest/dataset/manage/PermissionManage.java",
                List.of("if (getRowPermissionsApi() == null)", "if (userEntity == null)", "Optional.ofNullable(userEntity.getRoleIds())"),
                List.of("getRowPermissionsApi().getUserById(userId);\n        List<Long> roleIds"));
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
    void tableHeaderGroupConfigShouldGuardMissingMeta() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/editor-style/components/table/TableHeaderGroupConfig.vue",
                List.of(
                        "if (startIndex === -1 || !curCol?.children) return",
                        "if (startIndex === undefined || startIndex === -1 || !curCol?.children) return",
                        "if (index !== -1)",
                        "const inputValue = cellMeta?.name || curCell.getMeta().label || cellField",
                        "if (cellMeta) {",
                        "curMeta.push({\n                field: cellField"
                ),
                List.of(
                        "curMeta.splice(index, 1)\n          s2.setDataCfg",
                        "inputValue: cellMeta.name",
                        "cellMeta.name = res.value\n            s2.setDataCfg"
                ));
    }

    @Test
    void editorInsertionShouldGuardMissingCodeMirrorSelection() throws IOException {
        assertFrontendTreeDoesNotContain(List.of(
                "selection.ranges[0]",
                "renameInput.value[0].focus()"
        ));
        assertFrontendContract("core/core-frontend/src/views/visualized/data/dataset/form/AddSql.vue",
                List.of(
                        "const getCursor = view =>",
                        "if (!codeCom.value) return",
                        "const from = getCursor(codeCom.value)",
                        "changes: { from: 0, to, insert: value }"
                ),
                List.of());
        List.of(
                "core/core-frontend/src/views/visualized/data/dataset/form/CalcFieldEdit.vue",
                "core/core-frontend/src/views/chart/components/editor/editor-style/components/table/CustomAggrEdit.vue",
                "core/core-frontend/src/components/visualization/JumpSetOuterContentEditor.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const getCursor = view =>",
                                "if (!mirror.value) return",
                                "const from = getCursor(mirror.value)",
                                "selection: { anchor: from + value.length }"
                        ),
                        List.of());
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
    }

    @Test
    void visualizationDialogsShouldGuardEmptyPanelTreeAndColorCases() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/editor-style/components/ColorSelector.vue",
                List.of(
                        "const getDefaultColorCase = () =>",
                        "const selectedColorCase =",
                        "state.customColor = state.colorForm.colors[0] || null"
                ),
                List.of("items[0].colors"));
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/editor-style/components/CustomColorStyleSelect.vue",
                List.of(
                        "const selectedColorCase =",
                        "const colors = selectedColorCase?.colors || []",
                        "state.value.basicStyleForm[colorSchemeName.value] = selectedColorCase.value"
                ),
                List.of("items[0].colors"));
        assertFrontendContract("core/core-frontend/src/components/color-scheme/src/ColorScheme.vue",
                List.of(
                        "const getSelectedColorCase =",
                        "const selectedColorCase = getSelectedColorCase(colorScheme.value.name)",
                        "JSON.stringify(selectedColorCase?.colors || [])"
                ),
                List.of("items[0].colors"));
        List.of(
                "core/core-frontend/src/components/visualization/LinkJumpSet.vue",
                "core/core-frontend/src/custom-component/screen/SelectScreenDialog.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const panelList = Array.isArray(rsp) ? rsp : []",
                                "state.panelList = panelList[0].children || []"
                        ),
                        List.of("state.panelList = rsp[0].children"));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
    }

    @Test
    void visualizationUserLookupShouldUseCurrentUserTable() throws IOException {
        assertFrontendContract("core/core-backend/src/main/resources/mybatis/ExtDataVisualizationMapper.xml",
                List.of("from iam_user where id"),
                List.of("from per_user"));
    }

    @Test
    void visualizationRelationSettingsShouldGuardEmptyCollections() throws IOException {
        assertFrontendContract("core/core-frontend/src/components/visualization/OuterParamsSet.vue",
                List.of(
                        "const outerParams = rsp.data || {}",
                        "outerParams.outerParamsInfoArray.filter(item => item?.paramsInfoId)",
                        "outerParamsInfoTree.value?.setCurrentKey(firstNode.paramsInfoId)",
                        "const targetViewInfoList = outerParamsInfo.targetViewInfoList || []",
                        "targetViewInfoList.forEach",
                        "datasetInfo.datasetViews?.forEach",
                        "state.currentLinkPanelViewArray = rsp.data || []",
                        "if (!data?.paramsInfoId) return"
                ),
                List.of(
                        "state.outerParamsInfoArray = state.outerParams?.outerParamsInfoArray",
                        "outerParamsInfo.targetViewInfoList.forEach",
                        "datasetInfo.datasetViews.forEach",
                        "outerParamsInfoTree.value.setCurrentKey(firstNode.paramsInfoId)",
                        "state.currentLinkPanelViewArray = rsp.data\n"
                ));
        assertFrontendContract("core/core-frontend/src/components/visualization/LinkageSet.vue",
                List.of(
                        "linkageTree?.setCurrentKey(firstNode.targetViewId)",
                        "nodeClick(firstNode)",
                        "nodeClick(null)",
                        "linkageInfoTree.value?.setCurrentKey(data.targetViewId)",
                        "linkageInfoTreeDiffDs.value?.setCurrentKey(null)"
                ),
                List.of(
                        "linkageTree.setCurrentKey(firstNode.targetViewId)",
                        "nodeClick(firstNode)\n      sameDsTreeSelectedChange()",
                        "linkageInfoTree.value.setCurrentKey(data.targetViewId)"
                ));
    }

    @Test
    void queryAndTabComponentsShouldGuardLegacyEmptyState() throws IOException {
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/QueryUtils.ts",
                List.of(
                        "const checkedFields = next.checkedFields || []",
                        "const checkedFieldsMap = next.checkedFieldsMap || {}",
                        "Object.entries(checkedFieldsMap)",
                        "const treeFieldList = next.treeFieldList || []",
                        "Object.entries(item.checkedFieldsMap || {})",
                        "(item.checkedFields || []).includes(ele[0])"
                ),
                List.of(
                        "Object.entries(next.checkedFieldsMap)",
                        "next.checkedFields.includes(ele[0])",
                        "next.treeFieldList.map",
                        "Object.entries(item.checkedFieldsMap).filter"
                ));
        assertFrontendContract("core/core-frontend/src/custom-component/v-query/Component.vue",
                List.of(
                        "(next.checkedFields || []).includes(itx)",
                        "const defaultValueFirstItemMap = (propValue || []).reduce",
                        "defaultValueFirstItemMap[ele.datasetId?.split('--')[1]]"
                ),
                List.of(
                        "next.checkedFields.includes(itx)",
                        "const defaultValueFirstItemMap = propValue.reduce",
                        "defaultValueFirstItemMap[ele.datasetId.split('--')[1]]"
                ));
        List.of(
                "core/core-frontend/src/custom-component/tabs/Component.vue",
                "core/core-frontend/src/custom-component/screen/Component.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of("currentInstance?.refs?.['tabCanvas_' + index]?.[0]"),
                        List.of("currentInstance.refs['tabCanvas_' + index][0]"));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
        assertFrontendContract("core/core-frontend/src/custom-component/screen/Component.vue",
                List.of(
                        "const tabs = element.value.propValue || []",
                        "if (!tabs.length) return",
                        "const nowIndex = switchCount % tabs.length",
                        "element.value.editableTabsValue = tabs[nowIndex].name"
                ),
                List.of(
                        "switchCount % element.value.propValue.length",
                        "element.value.editableTabsValue = element.value.propValue[nowIndex].name"
                ));
    }

    @Test
    void chartEditorShouldGuardUnknownChartTypeConfig() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/chart/components/editor/index.vue",
                List.of(
                        "const currentChartTypeOption = computed(() => state.chartTypeOptions[0])",
                        "const viewConfig = getViewConfig(newVal[0])",
                        "state.chartTypeOptions.splice(0, state.chartTypeOptions.length, ...(viewConfig ? [viewConfig] : []))",
                        "if (!viewConf) return",
                        "v-if=\"currentChartTypeOption?.isPlugin\"",
                        "v-else-if=\"currentChartTypeOption?.icon\"",
                        "iconChartMap[currentChartTypeOption.icon]"
                ),
                List.of(
                        "state.chartTypeOptions.splice(0, state.chartTypeOptions.length, getViewConfig(newVal[0]))",
                        "state.chartTypeOptions[0].icon",
                        "const viewConf = getViewConfig(type)\n  if (viewConf.isPlugin)"
                ));
    }

    @Test
    void dynamicDataComponentsShouldGuardEmptyChartRows() throws IOException {
        List.of(
                "core/core-frontend/src/custom-component/rich-text/CrestRichTextView.vue",
                "core/core-frontend/src/custom-component/picture-group/Component.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const sourceFields = Array.isArray(chartDetails.data?.sourceFields)",
                                "const dataFields = Array.isArray(chartDetails.data?.fields) ? chartDetails.data.fields : []",
                                "const rowData = chartDetails.data?.tableRow?.[0] || {}",
                                "curViewInfo['curFields'] = res.data.fields || []"
                        ),
                        List.of(
                                "chartDetails.data.sourceFields.forEach",
                                "chartDetails.data.fields.reduce",
                                "const rowData = chartDetails.data.tableRow[0]",
                                "curViewInfo['curFields'] = res.data.fields\n"
                        ));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
        assertFrontendContract("core/core-frontend/src/custom-component/rich-text/CrestRichTextView.vue",
                List.of("const yAxisItems = Array.isArray(chartDetails.yAxis) ? chartDetails.yAxis : []"),
                List.of("JSON.parse(JSON.stringify(chartDetails.yAxis))"));
    }

    @Test
    void tabComponentsShouldGuardMissingPropValue() throws IOException {
        List.of(
                "core/core-frontend/src/custom-component/tabs/Component.vue",
                "core/core-frontend/src/custom-component/screen/Component.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const tabItems = computed(() => (Array.isArray(element.value?.propValue) ? element.value.propValue : []))",
                                "const ensureTabItems = () => {",
                                "if (!Array.isArray(element.value.propValue))",
                                "ensureTabItems().push(newTab)",
                                "const tabs = tabItems.value",
                                "element.value.editableTabsValue = tabs[0].name"
                        ),
                        List.of(
                                "v-for=\"tabItem in element.propValue\"",
                                "v-for=\"(tabItem, index) in element.propValue\"",
                                "element.value.propValue.length > 0",
                                "element.value.editableTabsValue = element.value.propValue[0].name"
                        ));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
    }

    @Test
    void routeEntrypointsShouldGuardEmptyChildren() throws IOException {
        List.of(
                "core/core-frontend/src/layout/components/SystemCfg.vue",
                "core/core-frontend/src/layout/components/DesktopSetting.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const kidPath = sysMenu.matched?.[0]?.children?.[0]?.path",
                                "push(kidPath ? `${sysMenu.path}/${kidPath}` : sysMenu.path)"
                        ),
                        List.of(
                                "sysMenu.matched[0].children[0].path",
                                "push(`${sysMenu.path}/${kidPath}`)"
                        ));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
        assertFrontendContract("core/core-frontend/src/router/establish.ts",
                List.of(
                        "if (!ele) {",
                        "if (ele.children?.length) {",
                        "if (item.children?.length) {",
                        "return path || item.path"
                ),
                List.of(
                        "if (ele.children) {\n      getDefaultPath(ele.children[0])",
                        "if (item.children) {\n    getDefaultPath(item.children[0], item)"
                ));
    }

    @Test
    void shareAndChartRuntimeHelpersShouldGuardMissingDomAndGeometry() throws IOException {
        List.of(
                "core/core-frontend/src/views/share/share/ShareHandler.vue",
                "core/core-frontend/src/views/share/share/ShareVisualHead.vue"
        ).forEach(relativePath -> {
            try {
                assertFrontendContract(relativePath,
                        List.of(
                                "const checkboxBorder = e?.children?.[0]?.children?.[1] as HTMLElement | undefined",
                                "const messageContainer = e?.children?.[1]?.children?.[0] as HTMLElement | undefined",
                                "const parentElement = e?.parentElement",
                                "if (!e || !parentElement)"
                        ),
                        List.of(
                                "e.children[0].children[1].style.borderColor",
                                "e.children[1].children[0].querySelector",
                                "e.children[1].children[0].appendChild",
                                "e.parentElement.querySelector"
                        ));
            } catch (IOException e) {
                throw new AssertionError("Unable to read " + relativePath, e);
            }
        });
        assertFrontendContract("core/core-frontend/src/views/share/share/TicketEdit.vue",
                List.of(
                        "const e = domRef?.input",
                        "const parentElement = e?.parentElement",
                        "if (!e || !parentElement)"
                ),
                List.of(
                        "const e = domRef.input",
                        "e.parentElement.querySelector"
                ));
        assertFrontendContract("core/core-frontend/src/views/chart/components/js/g2plot_tooltip_carousel.ts",
                List.of(
                        "const geometry = view.getGeometries()?.[0]",
                        "const elements = geometry?.elements || []",
                        "const point = elements[elements.length - 1]?.getModel?.()"
                ),
                List.of(".elements[view.getGeometries()?.[0].elements?.length - 1].getModel()"));
        assertFrontendContract("core/core-frontend/src/views/chart/components/js/extremumUitl.ts",
                List.of(
                        "const seriesLabelFormatterList = labelAttr.seriesLabelFormatter || []",
                        "const { yAxis = [] } = parseJson(chart)",
                        "const geometry = newChart.chart?.geometries?.[0]",
                        "geometry?.beforeMappingData?.forEach",
                        "newChart.chart?.geometries?.[0]?.on?.('afteranimate'",
                        "components?.[0]?.component?.cfg?.items || []",
                        "if (!containerElement) {",
                        "const geometries = ev.view?.getGeometries?.() || []"
                ),
                List.of(
                        "newChart.chart.geometries[0].on",
                        ".components[0].component.cfg.items",
                        "labelAttr.seriesLabelFormatter.find",
                        "labelAttr.seriesLabelFormatter[0]",
                        "ev.view\n    .getGeometries()"
                ));
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
