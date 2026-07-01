package io.crest.visualization.server;

import com.fasterxml.jackson.core.type.TypeReference;
import io.crest.api.dataset.union.DatasetTableInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.api.visualization.vo.DataVisualizationVO;
import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import io.crest.api.visualization.request.VisualizationAppExportRequest;
import io.crest.api.visualization.vo.AppCoreChartViewVO;
import io.crest.api.visualization.vo.AppCoreDatasetGroupVO;
import io.crest.api.visualization.vo.AppCoreDatasourceVO;
import io.crest.api.visualization.vo.VisualizationExport2AppVO;
import io.crest.api.visualization.vo.VisualizationLinkJumpInfoVO;
import io.crest.api.visualization.vo.VisualizationLinkJumpTargetViewInfoVO;
import io.crest.api.visualization.vo.VisualizationLinkJumpVO;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.ext.mapper.ExtChartViewMapper;
import io.crest.chart.manage.ChartDataManage;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.result.ResultCode;
import io.crest.utils.IDUtils;
import io.crest.utils.JsonUtil;
import io.crest.utils.SnowFlake;
import io.crest.template.dao.auto.mapper.VisualizationTemplateExtendDataMapper;
import io.crest.template.dao.ext.ExtVisualizationTemplateMapper;
import io.crest.visualization.dao.auto.entity.DataVisualizationInfo;
import io.crest.visualization.dao.auto.entity.SnapshotVisualizationLinkJump;
import io.crest.visualization.dao.auto.entity.SnapshotVisualizationLinkJumpInfo;
import io.crest.visualization.dao.auto.entity.SnapshotVisualizationLinkJumpTargetViewInfo;
import io.crest.visualization.dao.auto.mapper.DataVisualizationInfoMapper;
import io.crest.visualization.dao.auto.mapper.SnapshotVisualizationLinkJumpInfoMapper;
import io.crest.visualization.dao.auto.mapper.SnapshotVisualizationLinkJumpMapper;
import io.crest.visualization.dao.auto.mapper.SnapshotVisualizationLinkJumpTargetViewInfoMapper;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import io.crest.visualization.manage.CoreVisualizationManage;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataVisualizationServerImportTest {

    private DataVisualizationServer server;
    private VisualizationTemplateExtendDataMapper templateExtendDataMapper;
    private StaticResourceServer staticResourceServer;
    private CoreVisualizationManage coreVisualizationManage;
    private ChartDataManage chartDataManage;
    private ExtVisualizationTemplateMapper appTemplateMapper;
    private DataVisualizationInfoMapper visualizationInfoMapper;
    private ExtDataVisualizationMapper extDataVisualizationMapper;
    private ExtChartViewMapper extChartViewMapper;
    private SnapshotVisualizationLinkJumpMapper snapshotVisualizationLinkJumpMapper;
    private SnapshotVisualizationLinkJumpInfoMapper snapshotVisualizationLinkJumpInfoMapper;
    private SnapshotVisualizationLinkJumpTargetViewInfoMapper snapshotVisualizationLinkJumpTargetViewInfoMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(IDUtils.class, "snowFlake", new SnowFlake(1, 1));
        templateExtendDataMapper = mock(VisualizationTemplateExtendDataMapper.class);
        staticResourceServer = mock(StaticResourceServer.class);
        coreVisualizationManage = mock(CoreVisualizationManage.class);
        chartDataManage = mock(ChartDataManage.class);
        appTemplateMapper = mock(ExtVisualizationTemplateMapper.class);
        visualizationInfoMapper = mock(DataVisualizationInfoMapper.class);
        extDataVisualizationMapper = mock(ExtDataVisualizationMapper.class);
        extChartViewMapper = mock(ExtChartViewMapper.class);
        snapshotVisualizationLinkJumpMapper = mock(SnapshotVisualizationLinkJumpMapper.class);
        snapshotVisualizationLinkJumpInfoMapper = mock(SnapshotVisualizationLinkJumpInfoMapper.class);
        snapshotVisualizationLinkJumpTargetViewInfoMapper = mock(SnapshotVisualizationLinkJumpTargetViewInfoMapper.class);
        server = new DataVisualizationServer();
        ReflectionTestUtils.setField(server, "templateExtendDataMapper", templateExtendDataMapper);
        ReflectionTestUtils.setField(server, "staticResourceServer", staticResourceServer);
        ReflectionTestUtils.setField(server, "coreVisualizationManage", coreVisualizationManage);
        ReflectionTestUtils.setField(server, "chartDataManage", chartDataManage);
        ReflectionTestUtils.setField(server, "appTemplateMapper", appTemplateMapper);
        ReflectionTestUtils.setField(server, "visualizationInfoMapper", visualizationInfoMapper);
        ReflectionTestUtils.setField(server, "extDataVisualizationMapper", extDataVisualizationMapper);
        ReflectionTestUtils.setField(server, "extChartViewMapper", extChartViewMapper);
        ReflectionTestUtils.setField(server, "snapshotVisualizationLinkJumpMapper", snapshotVisualizationLinkJumpMapper);
        ReflectionTestUtils.setField(server, "snapshotVisualizationLinkJumpInfoMapper", snapshotVisualizationLinkJumpInfoMapper);
        ReflectionTestUtils.setField(server, "snapshotVisualizationLinkJumpTargetViewInfoMapper", snapshotVisualizationLinkJumpTargetViewInfoMapper);

        DataVisualizationInfo current = new DataVisualizationInfo();
        current.setId(8000L);
        current.setType("dataV");
        current.setCreateBy("2");
        when(visualizationInfoMapper.selectById(8000L)).thenReturn(current);

        DataVisualizationVO dvInfo = new DataVisualizationVO();
        dvInfo.setComponentData("[{\"id\":\"100\"},{\"id\":\"101\"}]");
        when(extDataVisualizationMapper.findDvInfo(eq(8000L), eq(null), any())).thenReturn(dvInfo);
        when(extChartViewMapper.selectListCustom(eq(8000L), any()))
                .thenReturn(List.of(chartView(100L), chartView(101L)));

        AppCoreChartViewVO chart100 = new AppCoreChartViewVO();
        chart100.setId(100L);
        chart100.setSceneId(8000L);
        chart100.setTableId(200L);
        AppCoreChartViewVO chart101 = new AppCoreChartViewVO();
        chart101.setId(101L);
        chart101.setSceneId(8000L);
        chart101.setTableId(200L);
        when(appTemplateMapper.findAppViewInfo(any())).thenReturn(List.of(chart100, chart101));
    }

    private CoreChartView chartView(Long id) {
        CoreChartView view = new CoreChartView();
        view.setId(id);
        view.setSceneId(8000L);
        view.setTableId(200L);
        return view;
    }

    @Test
    void decompressionLocalFileShouldParseDet2AppTemplate() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "risk-screen-APP.DET2APP",
                "application/octet-stream",
                det2AppJson().getBytes(StandardCharsets.UTF_8)
        );

        DataVisualizationVO result = server.decompressionLocalFile(file);

        assertNotNull(result);
        assertEquals("经营分析演示", result.getName());
        assertEquals("dataV", result.getType());
        assertEquals(3, result.getVersion());
        assertNotNull(result.getAppData());
        assertEquals("100", result.getAppData().getViewIdMap().keySet().iterator().next());
        Long newViewId = result.getCanvasViewInfo().keySet().iterator().next();
        assertNotEquals(100L, newViewId);
        assertTrue(result.getComponentData().contains(String.valueOf(newViewId)));
        assertFalse(result.getComponentData().contains("\"id\":\"100\""));
        verify(templateExtendDataMapper).insert(any());
        verify(staticResourceServer).saveFilesToServe(eq("{\"/static-resource/logo.png\":\"data:image/png;base64,AAA\"}"));
    }

    @Test
    void decompressionLocalFileShouldWriteImportedComponentIdsAsStrings() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "risk-screen-APP.DET2APP",
                "application/octet-stream",
                det2AppJsonWithNumericComponentId().getBytes(StandardCharsets.UTF_8)
        );

        DataVisualizationVO result = server.decompressionLocalFile(file);

        Long newViewId = result.getCanvasViewInfo().keySet().iterator().next();
        assertTrue(result.getComponentData().contains("\"id\":\"" + newViewId + "\""));
        assertFalse(result.getComponentData().contains("\"id\":" + newViewId));
    }

    @Test
    void decompressionLocalFileShouldRejectUnsupportedFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "risk-screen.json",
                "application/json",
                det2AppJson().getBytes(StandardCharsets.UTF_8)
        );

        CrestException exception = assertThrows(CrestException.class, () -> server.decompressionLocalFile(file));

        assertEquals("仅支持 DET2 或 DET2APP 模板文件", exception.getMsg());
    }

    @Test
    void decompressionLocalFileShouldRejectOversizedTemplateBeforeReadingContent() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(50L * 1024L * 1024L + 1L);
        when(file.getOriginalFilename()).thenReturn("risk-screen.det2");

        CrestException exception = assertThrows(CrestException.class, () -> server.decompressionLocalFile(file));

        assertEquals("模板文件不能超过50MB", exception.getMsg());
        verify(file, never()).getBytes();
    }

    @Test
    void saveCanvasShouldPersistImportedInnerJumpTargets() throws Exception {
        when(coreVisualizationManage.innerSave(any())).thenReturn(9000L);

        DataVisualizationBaseRequest request = new DataVisualizationBaseRequest();
        request.setName("经营分析演示");
        request.setPid(0L);
        request.setType("dataV");
        request.setDataType("dataset");
        request.setComponentData("[{\"id\":\"900100\"}]");
        ChartViewDTO chartView = new ChartViewDTO();
        chartView.setId(900100L);
        request.setCanvasViewInfo(Map.of(900100L, chartView));
        request.setAppData(appDataWithInnerJump());

        String newDvId = server.saveCanvas(request);

        assertEquals("9000", newDvId);
        ArgumentCaptor<SnapshotVisualizationLinkJump> jumpCaptor = ArgumentCaptor.forClass(SnapshotVisualizationLinkJump.class);
        ArgumentCaptor<SnapshotVisualizationLinkJumpInfo> infoCaptor = ArgumentCaptor.forClass(SnapshotVisualizationLinkJumpInfo.class);
        ArgumentCaptor<SnapshotVisualizationLinkJumpTargetViewInfo> targetCaptor = ArgumentCaptor.forClass(SnapshotVisualizationLinkJumpTargetViewInfo.class);
        verify(snapshotVisualizationLinkJumpMapper).insert(jumpCaptor.capture());
        verify(snapshotVisualizationLinkJumpInfoMapper).insert(infoCaptor.capture());
        verify(snapshotVisualizationLinkJumpTargetViewInfoMapper).insert(targetCaptor.capture());

        SnapshotVisualizationLinkJump jump = jumpCaptor.getValue();
        SnapshotVisualizationLinkJumpInfo jumpInfo = infoCaptor.getValue();
        SnapshotVisualizationLinkJumpTargetViewInfo targetInfo = targetCaptor.getValue();
        assertEquals(9000L, jump.getSourceDvId());
        assertEquals(900100L, jump.getSourceViewId());
        assertEquals(jump.getId(), jumpInfo.getLinkJumpId());
        assertEquals("inner", jumpInfo.getLinkType());
        assertEquals(9200L, jumpInfo.getTargetDvId());
        assertEquals(jumpInfo.getId(), targetInfo.getLinkJumpInfoId());
        assertEquals("900101", targetInfo.getTargetViewId());
        assertEquals("view", targetInfo.getTargetType());
    }

    @Test
    void saveCanvasShouldRejectAppWithoutDatasetMapping() {
        DataVisualizationBaseRequest request = baseAppSaveRequest("dataset");
        AppCoreDatasetGroupVO datasetGroup = new AppCoreDatasetGroupVO();
        datasetGroup.setId(100L);
        datasetGroup.setName("演示数据集");
        datasetGroup.setNodeType("dataset");
        VisualizationExport2AppVO appData = emptyAppData();
        appData.setDatasetGroupsInfo(List.of(datasetGroup));
        request.setAppData(appData);

        CrestException exception = assertThrows(CrestException.class, () -> server.saveCanvas(request));

        assertEquals(ResultCode.PARAM_IS_INVALID.code(), exception.getCode());
        assertEquals("导入应用未完成数据集匹配，请先选择目标数据集：演示数据集", exception.getMsg());
    }

    @Test
    void saveCanvasShouldRejectAppWithoutDatasourceMapping() {
        DataVisualizationBaseRequest request = baseAppSaveRequest("datasource");
        AppCoreDatasourceVO datasource = new AppCoreDatasourceVO();
        datasource.setId(200L);
        datasource.setName("Crest 综合演示数据仓库");
        datasource.setType("mysql");
        VisualizationExport2AppVO appData = emptyAppData();
        appData.setDatasourceInfo(List.of(datasource));
        request.setAppData(appData);

        CrestException exception = assertThrows(CrestException.class, () -> server.saveCanvas(request));

        assertEquals(ResultCode.PARAM_IS_INVALID.code(), exception.getCode());
        assertEquals("导入应用未完成数据源匹配，请先选择目标数据源：Crest 综合演示数据仓库", exception.getMsg());
    }

    @Test
    void export2AppCheckShouldIncludeLinkJumpTargets() {
        VisualizationAppExportRequest request = new VisualizationAppExportRequest();
        request.setDvId(8000L);
        request.setViewIds(List.of(100L));
        request.setDsIds(List.of(200L));

        AppCoreDatasourceVO datasource = new AppCoreDatasourceVO();
        datasource.setId(1L);
        datasource.setType("MySQL");
        VisualizationLinkJumpVO linkJump = new VisualizationLinkJumpVO();
        linkJump.setId(10L);
        linkJump.setSourceDvId(8000L);
        linkJump.setSourceViewId(100L);
        VisualizationLinkJumpInfoVO linkJumpInfo = new VisualizationLinkJumpInfoVO();
        linkJumpInfo.setId(20L);
        linkJumpInfo.setLinkJumpId(10L);
        linkJumpInfo.setLinkType("inner");
        VisualizationLinkJumpTargetViewInfoVO targetViewInfo = new VisualizationLinkJumpTargetViewInfoVO();
        targetViewInfo.setTargetId(30L);
        targetViewInfo.setLinkJumpInfoId(20L);
        targetViewInfo.setTargetViewId("101");

        when(appTemplateMapper.findAppDatasourceInfo(List.of(200L))).thenReturn(List.of(datasource));
        when(appTemplateMapper.findAppLinkJumpInfo(8000L)).thenReturn(List.of(linkJump));
        when(appTemplateMapper.findAppLinkJumpInfoInfo(8000L)).thenReturn(List.of(linkJumpInfo));
        when(appTemplateMapper.findAppLinkJumpTargetViewInfoInfo(8000L)).thenReturn(List.of(targetViewInfo));

        VisualizationExport2AppVO result = server.export2AppCheck(request);

        assertTrue(result.getCheckStatus());
        assertEquals(1, result.getLinkJumps().size());
        assertEquals(1, result.getLinkJumpInfos().size());
        assertEquals(1, result.getLinkJumpTargetInfos().size());
        assertEquals("inner", result.getLinkJumpInfos().get(0).getLinkType());
        assertEquals("101", result.getLinkJumpTargetInfos().get(0).getTargetViewId());
    }

    @Test
    void export2AppCheckShouldUseSnapshotLinkJumpsWhenPresent() {
        VisualizationAppExportRequest request = new VisualizationAppExportRequest();
        request.setDvId(8000L);
        request.setViewIds(List.of(100L));
        request.setDsIds(List.of(200L));

        AppCoreDatasourceVO datasource = new AppCoreDatasourceVO();
        datasource.setId(1L);
        datasource.setType("MySQL");
        VisualizationLinkJumpVO linkJump = new VisualizationLinkJumpVO();
        linkJump.setId(10L);
        linkJump.setSourceDvId(8000L);
        VisualizationLinkJumpInfoVO linkJumpInfo = new VisualizationLinkJumpInfoVO();
        linkJumpInfo.setId(20L);
        linkJumpInfo.setLinkJumpId(10L);
        linkJumpInfo.setLinkType("inner");

        when(appTemplateMapper.findAppDatasourceInfo(List.of(200L))).thenReturn(List.of(datasource));
        when(appTemplateMapper.findAppSnapshotLinkJumpInfo(8000L)).thenReturn(List.of(linkJump));
        when(appTemplateMapper.findAppSnapshotLinkJumpInfoInfo(8000L)).thenReturn(List.of(linkJumpInfo));

        VisualizationExport2AppVO result = server.export2AppCheck(request);

        assertEquals(1, result.getLinkJumps().size());
        assertEquals(1, result.getLinkJumpInfos().size());
        assertEquals("inner", result.getLinkJumpInfos().get(0).getLinkType());
        verify(appTemplateMapper, never()).findAppLinkJumpInfo(8000L);
        verify(appTemplateMapper, never()).findAppLinkJumpInfoInfo(8000L);
    }

    @Test
    void export2AppCheckShouldUseSnapshotViewsWhenPublishedViewsAreMissing() {
        VisualizationAppExportRequest request = new VisualizationAppExportRequest();
        request.setDvId(8000L);
        request.setViewIds(List.of(100L));
        request.setDsIds(List.of(200L));

        AppCoreDatasourceVO datasource = new AppCoreDatasourceVO();
        datasource.setId(1L);
        datasource.setType("MySQL");
        AppCoreChartViewVO snapshotView = new AppCoreChartViewVO();
        snapshotView.setId(100L);
        snapshotView.setSceneId(8000L);
        snapshotView.setTitle("关联需求数");

        when(appTemplateMapper.findAppViewInfo(List.of(100L))).thenReturn(new ArrayList<>());
        when(appTemplateMapper.findAppSnapshotViewInfo(List.of(100L))).thenReturn(List.of(snapshotView));
        when(appTemplateMapper.findAppDatasourceInfo(List.of(200L))).thenReturn(List.of(datasource));

        VisualizationExport2AppVO result = server.export2AppCheck(request);

        assertEquals(1, result.getChartViewsInfo().size());
        assertEquals("关联需求数", result.getChartViewsInfo().get(0).getTitle());
    }

    @Test
    void export2AppCheckShouldFillMissingViewsFromSnapshot() {
        VisualizationAppExportRequest request = new VisualizationAppExportRequest();
        request.setDvId(8000L);
        request.setViewIds(List.of(100L, 101L));
        request.setDsIds(List.of(200L));

        AppCoreDatasourceVO datasource = new AppCoreDatasourceVO();
        datasource.setId(1L);
        datasource.setType("MySQL");
        AppCoreChartViewVO publishedView = new AppCoreChartViewVO();
        publishedView.setId(100L);
        publishedView.setSceneId(8000L);
        publishedView.setTableId(200L);
        publishedView.setTitle("指标总数");
        AppCoreChartViewVO snapshotView = new AppCoreChartViewVO();
        snapshotView.setId(101L);
        snapshotView.setSceneId(8000L);
        snapshotView.setTitle("关联需求数");

        when(appTemplateMapper.findAppViewInfo(List.of(100L, 101L))).thenReturn(List.of(publishedView));
        when(appTemplateMapper.findAppSnapshotViewInfo(List.of(101L))).thenReturn(List.of(snapshotView));
        when(appTemplateMapper.findAppDatasourceInfo(List.of(200L))).thenReturn(List.of(datasource));

        VisualizationExport2AppVO result = server.export2AppCheck(request);

        assertEquals(2, result.getChartViewsInfo().size());
        assertEquals("指标总数", result.getChartViewsInfo().get(0).getTitle());
        assertEquals("关联需求数", result.getChartViewsInfo().get(1).getTitle());
    }

    @Test
    void importSqlTableMappingShouldPreferLongestSourceTableName() {
        Map<String, String> tableNameMap = new LinkedHashMap<>();
        tableNameMap.put("v_risk", "excel_summary");
        tableNameMap.put("v_risk_detail", "excel_detail");

        String mappedSql = ReflectionTestUtils.invokeMethod(
                server,
                "replaceTableNames",
                "select * from v_risk_detail where exists (select 1 from v_risk)",
                tableNameMap
        );

        assertEquals("select * from excel_detail where exists (select 1 from excel_summary)", mappedSql);
    }

    @Test
    void importSqlTableMappingShouldRewriteDatasetTableAndGroupSql() {
        Map<String, String> tableNameMap = Map.of("template_source_sheet", "excel_Sheet0_abc123");
        String tableInfo = encodedSqlInfo(
                "template_source_sheet",
                "select project_code from template_source_sheet where project_code is not null"
        );

        String mappedTableInfo = ReflectionTestUtils.invokeMethod(server, "replaceDatasetTableSql", tableInfo, tableNameMap);
        DatasetTableInfoDTO mappedInfo = JsonUtil.parseObject(mappedTableInfo, DatasetTableInfoDTO.class);
        String mappedSql = new String(Base64.getDecoder().decode(mappedInfo.getSql()), StandardCharsets.UTF_8);

        assertEquals("excel_Sheet0_abc123", mappedInfo.getTable());
        assertEquals("select project_code from excel_Sheet0_abc123 where project_code is not null", mappedSql);

        DatasetTableDTO tableDTO = new DatasetTableDTO();
        tableDTO.setTableName("template_source_sheet");
        tableDTO.setInfo(tableInfo);
        UnionDTO unionDTO = new UnionDTO();
        unionDTO.setCurrentDs(tableDTO);

        String mappedGroupInfo = ReflectionTestUtils.invokeMethod(
                server,
                "replaceDatasetGroupSql",
                JsonUtil.toJSONString(List.of(unionDTO)).toString(),
                tableNameMap
        );
        UnionDTO mappedUnion = JsonUtil.parseList(mappedGroupInfo, new TypeReference<List<UnionDTO>>() {}).get(0);
        DatasetTableInfoDTO mappedGroupTableInfo = JsonUtil.parseObject(mappedUnion.getCurrentDs().getInfo(), DatasetTableInfoDTO.class);
        String mappedGroupSql = new String(Base64.getDecoder().decode(mappedGroupTableInfo.getSql()), StandardCharsets.UTF_8);

        assertEquals("excel_Sheet0_abc123", mappedUnion.getCurrentDs().getTableName());
        assertEquals("select project_code from excel_Sheet0_abc123 where project_code is not null", mappedGroupSql);
    }

    @Test
    void importIdMappingShouldNotRewriteEncodedDatasetSql() {
        String tableInfo = encodedSqlInfo(
                "template_source_sheet",
                "select 1263012001 as original_id from template_source_sheet"
        );
        DatasetTableDTO tableDTO = new DatasetTableDTO();
        tableDTO.setId(1263012001L);
        tableDTO.setTableName("template_source_sheet");
        tableDTO.setInfo(tableInfo);
        UnionDTO unionDTO = new UnionDTO();
        unionDTO.setCurrentDs(tableDTO);

        String mappedGroupInfo = ReflectionTestUtils.invokeMethod(
                server,
                "replaceMappedIdsPreservingDatasetSql",
                JsonUtil.toJSONString(List.of(unionDTO)).toString(),
                Map.of(1263012001L, 1263999001L)
        );
        String mappedTableInfo = ReflectionTestUtils.invokeMethod(
                server,
                "replaceDatasetGroupSql",
                mappedGroupInfo,
                Map.of("template_source_sheet", "excel_Sheet0_abc123")
        );

        UnionDTO mappedUnion = JsonUtil.parseList(mappedTableInfo, new TypeReference<List<UnionDTO>>() {}).get(0);
        DatasetTableInfoDTO mappedInfo = JsonUtil.parseObject(mappedUnion.getCurrentDs().getInfo(), DatasetTableInfoDTO.class);
        String mappedSql = new String(Base64.getDecoder().decode(mappedInfo.getSql()), StandardCharsets.UTF_8);

        assertEquals(1263999001L, mappedUnion.getCurrentDs().getId());
        assertEquals("excel_Sheet0_abc123", mappedInfo.getTable());
        assertEquals("select 1263012001 as original_id from excel_Sheet0_abc123", mappedSql);
    }

    private DataVisualizationBaseRequest baseAppSaveRequest(String dataType) {
        DataVisualizationBaseRequest request = new DataVisualizationBaseRequest();
        request.setName("经营分析演示");
        request.setPid(0L);
        request.setType("dataV");
        request.setDataType(dataType);
        request.setComponentData("[]");
        request.setCanvasViewInfo(new HashMap<>());
        return request;
    }

    private VisualizationExport2AppVO emptyAppData() {
        VisualizationExport2AppVO appData = new VisualizationExport2AppVO();
        appData.setDatasetGroupsInfo(new ArrayList<>());
        appData.setDatasetTablesInfo(new ArrayList<>());
        appData.setDatasetTableFieldsInfo(new ArrayList<>());
        appData.setDatasourceInfo(new ArrayList<>());
        appData.setLinkages(new ArrayList<>());
        appData.setLinkageFields(new ArrayList<>());
        appData.setLinkJumps(new ArrayList<>());
        appData.setLinkJumpInfos(new ArrayList<>());
        appData.setLinkJumpTargetInfos(new ArrayList<>());
        return appData;
    }

    private VisualizationExport2AppVO appDataWithInnerJump() {
        VisualizationExport2AppVO appData = new VisualizationExport2AppVO();
        appData.setDatasetGroupsInfo(new ArrayList<>());
        appData.setDatasetTablesInfo(new ArrayList<>());
        appData.setDatasetTableFieldsInfo(new ArrayList<>());
        appData.setDatasourceInfo(new ArrayList<>());
        appData.setLinkages(new ArrayList<>());
        appData.setLinkageFields(new ArrayList<>());
        Map<String, String> viewIdMap = new HashMap<>();
        viewIdMap.put("100", "900100");
        viewIdMap.put("101", "900101");
        appData.setViewIdMap(viewIdMap);

        VisualizationLinkJumpVO linkJump = new VisualizationLinkJumpVO();
        linkJump.setId(10L);
        linkJump.setSourceDvId(8000L);
        linkJump.setSourceViewId(100L);
        linkJump.setChecked(true);
        appData.setLinkJumps(List.of(linkJump));

        VisualizationLinkJumpInfoVO linkJumpInfo = new VisualizationLinkJumpInfoVO();
        linkJumpInfo.setId(20L);
        linkJumpInfo.setLinkJumpId(10L);
        linkJumpInfo.setLinkType("inner");
        linkJumpInfo.setJumpType("popup");
        linkJumpInfo.setTargetDvId(9200L);
        linkJumpInfo.setSourceFieldId(300L);
        linkJumpInfo.setChecked(true);
        appData.setLinkJumpInfos(List.of(linkJumpInfo));

        VisualizationLinkJumpTargetViewInfoVO targetViewInfo = new VisualizationLinkJumpTargetViewInfoVO();
        targetViewInfo.setTargetId(30L);
        targetViewInfo.setLinkJumpInfoId(20L);
        targetViewInfo.setSourceFieldActiveId(300L);
        targetViewInfo.setTargetViewId("101");
        targetViewInfo.setTargetFieldId("301");
        targetViewInfo.setTargetType("view");
        appData.setLinkJumpTargetInfos(List.of(targetViewInfo));
        return appData;
    }

    private String encodedSqlInfo(String tableName, String sql) {
        DatasetTableInfoDTO infoDTO = new DatasetTableInfoDTO();
        infoDTO.setTable(tableName);
        infoDTO.setSql(Base64.getEncoder().encodeToString(sql.getBytes(StandardCharsets.UTF_8)));
        return JsonUtil.toJSONString(infoDTO).toString();
    }

    private String det2AppJson() {
        return """
                {
                  "name": "经营分析演示",
                  "templateType": "self",
                  "dvType": "dataV",
                  "nodeType": "app",
                  "version": 3,
                  "canvasStyleData": "{\\"width\\":1920,\\"height\\":1080,\\"screenAdaptor\\":\\"keep\\"}",
                  "componentData": "[{\\"id\\":\\"100\\",\\"component\\":\\"UserView\\",\\"innerType\\":\\"bar\\"}]",
                  "dynamicData": "{\\"100\\":{\\"id\\":100,\\"sceneId\\":900,\\"title\\":\\"关联需求数\\",\\"type\\":\\"bar\\",\\"tableId\\":200,\\"data\\":{\\"sourceData\\":[]}}}",
                  "staticResource": "{\\"/static-resource/logo.png\\":\\"data:image/png;base64,AAA\\"}",
                  "appData": "{\\"visualizationInfo\\":\\"{\\\\\\"id\\\\\\":900,\\\\\\"name\\\\\\":\\\\\\"经营分析演示\\\\\\",\\\\\\"type\\\\\\":\\\\\\"dataV\\\\\\"}\\",\\"datasourceInfo\\":[],\\"datasetGroupsInfo\\":[]}"
                }
                """;
    }

    private String det2AppJsonWithNumericComponentId() {
        return """
                {
                  "name": "经营分析演示",
                  "templateType": "self",
                  "dvType": "dataV",
                  "nodeType": "app",
                  "version": 3,
                  "canvasStyleData": "{\\"width\\":1920,\\"height\\":1080,\\"screenAdaptor\\":\\"keep\\"}",
                  "componentData": "[{\\"id\\":100,\\"component\\":\\"UserView\\",\\"innerType\\":\\"bar\\"}]",
                  "dynamicData": "{\\"100\\":{\\"id\\":100,\\"sceneId\\":900,\\"title\\":\\"指标数量\\",\\"type\\":\\"bar\\",\\"tableId\\":200,\\"data\\":{\\"sourceData\\":[]}}}",
                  "staticResource": "{}",
                  "appData": "{\\"visualizationInfo\\":\\"{\\\\\\"id\\\\\\":900,\\\\\\"name\\\\\\":\\\\\\"经营分析演示\\\\\\",\\\\\\"type\\\\\\":\\\\\\"dataV\\\\\\"}\\",\\"datasourceInfo\\":[],\\"datasetGroupsInfo\\":[]}"
                }
                """;
    }
}
