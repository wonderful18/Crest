package io.crest.visualization.server;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.Snapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.StringValue;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.api.dataset.union.DatasetTableInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.api.report.bo.DatasetPermissionTemplate;
import io.crest.api.template.dto.TemplateManageFileDTO;
import io.crest.api.template.dto.VisualizationTemplateExtendDataDTO;
import io.crest.api.visualization.DataVisualizationApi;
import io.crest.api.visualization.dto.VisualizationViewTableDTO;
import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import io.crest.api.visualization.request.VisualizationAppExportRequest;
import io.crest.api.visualization.request.VisualizationWorkbranchQueryRequest;
import io.crest.api.visualization.vo.*;
import io.crest.auth.CrestLinkPermit;
import io.crest.chart.dao.auto.entity.CoreChartView;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.chart.dao.ext.mapper.ExtChartViewMapper;
import io.crest.chart.manage.ChartDataManage;
import io.crest.chart.manage.ChartViewManege;
import io.crest.commons.constants.DataVisualizationConstants;
import io.crest.commons.constants.OptConstants;
import io.crest.constant.CommonConstants;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.dataset.dao.auto.entity.CoreDatasetGroup;
import io.crest.dataset.dao.auto.entity.CoreDatasetTable;
import io.crest.dataset.dao.auto.entity.CoreDatasetTableField;
import io.crest.dataset.dao.auto.mapper.CoreDatasetGroupMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableFieldMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableMapper;
import io.crest.dataset.manage.DatasetDataManage;
import io.crest.dataset.manage.DatasetGroupManage;
import io.crest.dataset.manage.DatasetSQLManage;
import io.crest.dataset.utils.DatasetUtils;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.crest.datasource.provider.ExcelUtils;
import io.crest.datasource.server.DatasourceServer;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.i18n.Translator;
import io.crest.log.CrestAudit;
import io.crest.menu.dao.auto.entity.CoreMenu;
import io.crest.model.BusiNodeRequest;
import io.crest.model.BusiNodeVO;
import io.crest.operation.manage.CoreOptRecentManage;
import io.crest.portal.DataPortalPermissionManage;
import io.crest.result.ResultCode;
import io.crest.system.manage.CoreUserManage;
import io.crest.template.dao.auto.entity.VisualizationTemplate;
import io.crest.template.dao.auto.entity.VisualizationTemplateExtendData;
import io.crest.template.dao.auto.mapper.VisualizationTemplateExtendDataMapper;
import io.crest.template.dao.auto.mapper.VisualizationTemplateMapper;
import io.crest.template.dao.ext.ExtVisualizationTemplateMapper;
import io.crest.template.manage.TemplateCenterManage;
import io.crest.utils.*;
import io.crest.visualization.dao.auto.entity.*;
import io.crest.visualization.dao.auto.mapper.*;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import io.crest.visualization.manage.CoreBusiManage;
import io.crest.visualization.manage.CoreVisualizationManage;
import io.crest.visualization.manage.ResourcePermissionManage;
import io.crest.visualization.utils.VisualizationUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/data-visualization")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DataVisualizationServer implements DataVisualizationApi {

    @Resource
    private DataVisualizationInfoMapper visualizationInfoMapper;

    @Resource
    private ChartViewManege chartViewManege;
    @Resource
    private CoreChartViewMapper coreChartViewMapper;

    @Resource
    private ExtDataVisualizationMapper extDataVisualizationMapper;

    @Resource
    private CoreVisualizationManage coreVisualizationManage;

    @Resource
    private ChartDataManage chartDataManage;

    @Resource
    private VisualizationTemplateMapper templateMapper;

    @Resource
    private TemplateCenterManage templateCenterManage;

    @Resource
    private StaticResourceServer staticResourceServer;

    @Resource
    private VisualizationTemplateExtendDataMapper templateExtendDataMapper;

    @Resource
    private CoreOptRecentManage coreOptRecentManage;

    @Resource
    private VisualizationWatermarkMapper watermarkMapper;

    @Resource
    private DatasetGroupManage datasetGroupManage;

    @Resource
    private DatasetDataManage datasetDataManage;

    @Resource
    private ExtVisualizationTemplateMapper appTemplateMapper;

    @Resource
    private CoreDatasetGroupMapper coreDatasetGroupMapper;

    @Resource
    private CoreDatasetTableMapper coreDatasetTableMapper;

    @Resource
    private CoreDatasetTableFieldMapper coreDatasetTableFieldMapper;

    @Resource
    private CoreDatasourceMapper coreDatasourceMapper;

    @Resource
    private CoreBusiManage coreBusiManage;

    @Value("${crest.version:1.5.0}")
    private String crestVersion;

    @Resource
    private CoreUserManage coreUserManage;
    @Resource
    private DatasourceServer datasourceServer;

    @Resource
    private SnapshotDataVisualizationInfoMapper snapshotMapper;
    @Resource
    private ExtChartViewMapper extChartViewMapper;
    @Resource
    private DatasetSQLManage datasetSQLManage;

    @Resource
    private SnapshotVisualizationLinkageMapper snapshotVisualizationLinkageMapper;

    @Resource
    private SnapshotVisualizationLinkageFieldMapper snapshotVisualizationLinkageFieldMapper;

    @Resource
    private SnapshotVisualizationLinkJumpMapper snapshotVisualizationLinkJumpMapper;

    @Resource
    private SnapshotVisualizationLinkJumpInfoMapper snapshotVisualizationLinkJumpInfoMapper;

    @Resource
    private SnapshotVisualizationLinkJumpTargetViewInfoMapper snapshotVisualizationLinkJumpTargetViewInfoMapper;

    @Resource
    private DataPortalPermissionManage dataPortalPermissionManage;


    @Override
    public DataVisualizationVO findCopyResource(Long dvId, String busiFlag) {
        DataVisualizationVO result = Objects.requireNonNull(CommonBeanFactory.proxy(this.getClass())).findById(new DataVisualizationBaseRequest(dvId, busiFlag, CommonConstants.RESOURCE_TABLE.SNAPSHOT, DataVisualizationConstants.QUERY_SOURCE.MAIN_EDIT));
        if (result != null && result.getPid() == -1) {
            return result;
        } else {
            return null;
        }
    }

    @CrestLinkPermit("#p0.id")
    @CrestAudit(id = "#p0.id", ot = LogOT.READ, stExp = "#p0.busiFlag")
    @Override
    public DataVisualizationVO findById(@RequestBody DataVisualizationBaseRequest request) {
        Long dvId = request.getId();
        String busiFlag = request.getBusiFlag();
        String resourceTable = request.getResourceTable();
        boolean mainEdit = DataVisualizationConstants.QUERY_SOURCE.MAIN_EDIT.equals(request.getSource());
        if (mainEdit && CrestPermissionUtils.currentUserId() != null) {
            DataVisualizationVO permissionSource = extDataVisualizationMapper.findDvInfo(dvId, busiFlag, CommonConstants.RESOURCE_TABLE.CORE);
            if (permissionSource == null) {
                permissionSource = extDataVisualizationMapper.findDvInfo(dvId, busiFlag, resourceTable);
            }
            if (permissionSource != null) {
                CrestPermissionUtils.requireCreator(permissionSource.getCreateBy());
            }
        }
        // 如果是编辑查询 则进行镜像检查
        if (mainEdit) {
            QueryWrapper<SnapshotDataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", dvId);
            queryWrapper.in("status", Arrays.asList(CommonConstants.DV_STATUS.UNPUBLISHED, CommonConstants.DV_STATUS.SAVED_UNPUBLISHED)); // 状态为0 未发布 和 2 已保存未发布的 不需要重置镜像
            if (!snapshotMapper.exists(queryWrapper)) {
                coreVisualizationManage.dvSnapshotRecover(dvId);
            }
        }
        DataVisualizationVO result = extDataVisualizationMapper.findDvInfo(dvId, busiFlag, resourceTable);
        if (result != null) {
            if (
                    mainEdit && CrestPermissionUtils.currentUserId() != null
            ) {
                CrestPermissionUtils.requireCreator(result.getCreateBy());
            } else if (
                    CrestPermissionUtils.currentUserId() != null
                            && !dataPortalPermissionManage.canReadPublishedVisualization(dvId, result.getType())
            ) {
                CrestPermissionUtils.requireCreator(result.getCreateBy());
            }
            // get creator
            String userName = coreUserManage.getUserName(Long.valueOf(result.getCreateBy()));
            if (StringUtils.isNotBlank(userName)) {
                result.setCreatorName(userName);
            }
            //获取图表信息
            List<ChartViewDTO> chartViewDTOS = chartViewManege.listBySceneId(dvId, resourceTable);
            if (!CollectionUtils.isEmpty(chartViewDTOS)) {
                // 增加过滤当前使用的图表信息
                Map<Long, ChartViewDTO> viewInfo = chartViewDTOS.stream().filter(item -> componentDataContainsView(result.getComponentData(), item.getId())).collect(Collectors.toMap(ChartViewDTO::getId, chartView -> chartView));
                result.setCanvasViewInfo(viewInfo);
            }
            VisualizationWatermark watermark = watermarkMapper.selectById("system_default");
            VisualizationWatermarkVO watermarkVO = new VisualizationWatermarkVO();
            BeanUtils.copyBean(watermarkVO, watermark);
            result.setWatermarkInfo(watermarkVO);

            if (DataVisualizationConstants.QUERY_SOURCE.REPORT.equals(request.getSource()) && request.getTaskId() != null) {
                //获取定时报告过自定义过滤组件信息
                List<VisualizationReportFilterVO> filterVOS = extDataVisualizationMapper.queryReportFilter(dvId, request.getTaskId());
                if (!CollectionUtils.isEmpty(filterVOS)) {
                    Map<Long, VisualizationReportFilterVO> reportFilterInfo = filterVOS.stream().collect(Collectors.toMap(VisualizationReportFilterVO::getFilterId, filterVo -> filterVo));
                    result.setReportFilterInfo(reportFilterInfo);
                }
            }
            if (ObjectUtils.isNotEmpty(request.getShowWatermark()) && !request.getShowWatermark()) {
                VisualizationWatermarkVO watermarkInfo = result.getWatermarkInfo();
                String settingContent = null;
                if (ObjectUtils.isNotEmpty(watermarkInfo) && StringUtils.isNotBlank(settingContent = watermarkInfo.getSettingContent())) {
                    Map map = JsonUtil.parse(settingContent, Map.class);
                    map.put("enable", false);
                    settingContent = JsonUtil.toJSONString(map).toString();
                    watermarkInfo.setSettingContent(settingContent);
                    result.setWatermarkInfo(watermarkInfo);
                }
            }
            result.setWeight(9);
            return result;
        } else {
            CrestException.throwException(Translator.get("i18n_resource_not_exists"));
        }
        return null;
    }

    private boolean componentDataContainsView(String componentData, Long viewId) {
        if (StringUtils.isBlank(componentData) || viewId == null) {
            return false;
        }
        String viewIdText = viewId.toString();
        try {
            List<Map<String, Object>> components = JsonUtil.parseList(componentData, new TypeReference<List<Map<String, Object>>>() {
            });
            return containsComponentId(components, viewIdText);
        } catch (Exception e) {
            return componentData.contains("\"id\":\"" + viewIdText + "\"")
                    || componentData.contains("\"id\": \"" + viewIdText + "\"");
        }
    }

    private boolean containsComponentId(Object value, String viewId) {
        if (value instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (viewId.equals(String.valueOf(id))) {
                return true;
            }
            for (Object item : map.values()) {
                if (containsComponentId(item, viewId)) {
                    return true;
                }
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsComponentId(item, viewId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void appDatasetMatch(VisualizationExport2AppVO appData, Map<Long, Long> datasourceIdMap, Map<Long, Long> dsGroupIdMap, Map<Long, Long> dsTableIdMap, Map<Long, Long> dsTableFieldsIdMap,Map<String, String> dsTableFieldsDatasetNameMap) {

        List<AppCoreDatasetGroupVO> sourceDatasetGroupList = appData.getDatasetGroupsInfo();
        List<AppCoreDatasetTableVO> sourceDatasetTableList = appData.getDatasetTablesInfo();
        List<AppCoreDatasetTableFieldVO> sourceDatasetTableFieldList = appData.getDatasetTableFieldsInfo();

        Map<Long, List<AppCoreDatasetTableVO>> sourceDatasetTableMap =
                CrestCollectionUtils.groupBy(sourceDatasetTableList, AppCoreDatasetTableVO::getDatasetGroupId);

        Map<Long, List<AppCoreDatasetTableFieldVO>> sourceDatasetTableFieldMap =
                CrestCollectionUtils.groupBy(sourceDatasetTableFieldList, AppCoreDatasetTableFieldVO::getDatasetTableId);

        Map<Long, List<AppCoreDatasetTableFieldVO>> sourceDatasetTableFieldMapGroup =
                CrestCollectionUtils.groupBy(sourceDatasetTableFieldList, AppCoreDatasetTableFieldVO::getDatasetGroupId);


        sourceDatasetGroupList.forEach(sourceDatasetGroup -> {
            Long systemDatasetGroupId = sourceDatasetGroup.getSystemDatasetId();
            Long sourceDatasetGroupId = sourceDatasetGroup.getId();
            if (systemDatasetGroupId == null) {
                throwAppImportMappingMissing("数据集", sourceDatasetGroup.getName());
            }
            // 获取 dsGroupIdMap
            dsGroupIdMap.put(sourceDatasetGroup.getId(), systemDatasetGroupId);
            CoreDatasetGroup systemDatasetGroup = coreDatasetGroupMapper.selectById(systemDatasetGroupId);
            if (systemDatasetGroup != null) {
                QueryWrapper<CoreDatasetTable> wrapper = new QueryWrapper<>();
                wrapper.eq("dataset_group_id", systemDatasetGroupId);
                List<CoreDatasetTable> systemDatasetTableList = coreDatasetTableMapper.selectList(wrapper);
                List<AppCoreDatasetTableVO> sourceDatasetTableListSub = sourceDatasetTableMap.get(sourceDatasetGroupId);
                if (systemDatasetTableList != null && sourceDatasetTableListSub != null) {
                    for (AppCoreDatasetTableVO sourceTable : sourceDatasetTableListSub) {
                        for (CoreDatasetTable systemTable : systemDatasetTableList) {
                            if (sourceTable.getTableName().equals(systemTable.getTableName())) {
                                // 获取dsTableIdMap datasourceIdMap
                                dsTableIdMap.put(sourceTable.getId(), systemTable.getId());
                                datasourceIdMap.put(sourceTable.getDatasourceId(), systemTable.getDatasourceId());

                                // 获取 dsTableFieldsIdMap
                                List<AppCoreDatasetTableFieldVO> sourceDatasetTableFieldListSub = sourceDatasetTableFieldMap.get(sourceTable.getId());

                                QueryWrapper<CoreDatasetTableField> wrapperField = new QueryWrapper<>();
                                wrapperField.eq("dataset_table_id", systemTable.getId());
                                List<CoreDatasetTableField> systemDatasetTableFieldSub = coreDatasetTableFieldMapper.selectList(wrapperField);

                                for (AppCoreDatasetTableFieldVO sourceTableField : sourceDatasetTableFieldListSub) {
                                    for (CoreDatasetTableField systemTableField : systemDatasetTableFieldSub) {
                                        if (sourceTableField.getOriginName().equals(systemTableField.getOriginName())) {
                                            // 获取dsTableIdMap datasourceIdMap
                                            dsTableFieldsIdMap.put(sourceTableField.getId(), systemTableField.getId());
                                            dsTableFieldsDatasetNameMap.put(sourceTableField.getEngineFieldName(),systemTableField.getEngineFieldName());
                                            break;
                                        }
                                    }
                                }

                                // 获取 dsTableFieldsIdMapGroup 进行二次匹配 解决计算字段没有tableId 问题
                                List<AppCoreDatasetTableFieldVO> sourceDatasetTableFieldListSubGroup = sourceDatasetTableFieldMapGroup.get(sourceTable.getDatasetGroupId());

                                QueryWrapper<CoreDatasetTableField> wrapperFieldGroup = new QueryWrapper<>();
                                wrapperFieldGroup.eq("dataset_group_id", systemTable.getDatasetGroupId());
                                List<CoreDatasetTableField> systemDatasetTableFieldSubGroup = coreDatasetTableFieldMapper.selectList(wrapperFieldGroup);

                                for (AppCoreDatasetTableFieldVO sourceTableField : sourceDatasetTableFieldListSubGroup) {
                                    for (CoreDatasetTableField systemTableField : systemDatasetTableFieldSubGroup) {
                                        if (dsTableFieldsIdMap.get(sourceTableField.getId())==null && sourceTableField.getName().equals(systemTableField.getName())) {
                                            // 获取dsTableIdMap datasourceIdMap
                                            dsTableFieldsIdMap.put(sourceTableField.getId(), systemTableField.getId());
                                            dsTableFieldsDatasetNameMap.put(sourceTableField.getEngineFieldName(),systemTableField.getEngineFieldName());
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

            }
        });


    }

    @CrestAudit(id = "#p0.id", pid = "#p0.pid", ot = LogOT.CREATE, stExp = "#p0.type")
    @Override
    @Transactional
    public String saveCanvas(@RequestBody DataVisualizationBaseRequest request) throws Exception {
        /*
         * 发布兼容逻辑
         * saveCanvas 为初次保存 包括 模板 应用 普通创建 所有变更操作都走snapshot表
         * 1.如果是文件夹直接保存在主表中，如果是仪表板（数据大屏），主表和镜像表各保存一份 主表仅作为权限和预览控制此时主表状态为‘未发布’
         * 2.编辑检查：如果存在未发布的仪表板snapshot，则默认加载snapshot进行编辑所有操作均为snapshot操作
         * 3.发布（重新发布）：将snapshot表中的所有数据复制到主表中，同时变更主表状态为‘已发布’
         * 4.如果对已发布的仪表板编辑并存在已保存的镜像，此时仪表板状态为‘已保存未发布’
         */
        boolean isAppSave = false;
        Long time = System.currentTimeMillis();
        // 如果是应用 则新进行应用校验 数据集名称和 数据源名称校验
        VisualizationExport2AppVO appData = request.getAppData();
        Map<Long, Long> dsGroupIdMap = new HashMap<>();
        List<DatasetGroupInfoDTO> newDsGroupInfo = new ArrayList<>();
        Map<Long, Long> dsTableIdMap = new HashMap<>();
        Map<Long, Long> dsTableFieldsIdMap = new HashMap<>();
        Map<String, String> dsTableFieldsDatasetNameMap = new HashMap<>();
        List<CoreDatasetTableField> dsTableFieldsList = new ArrayList();
        Map<Long, Long> datasourceIdMap = new HashMap<>();
        Map<Long, Map<String, String>> dsTableNamesMap = new HashMap<>();
        List<Long> newDatasourceId = new ArrayList<>();
        Map<Long, Long> linkageIdMap = new HashMap<>();
        Map<Long, Long> linkageFieldIdMap = new HashMap<>();
        Map<Long, Long> linkJumpIdMap = new HashMap<>();
        Map<Long, Long> linkJumpInfoIdMap = new HashMap<>();
        if (appData != null) {
            isAppSave = true;
            if ("dataset".equals(request.getDataType())) {
                appDatasetMatch(appData, datasourceIdMap, dsGroupIdMap, dsTableIdMap, dsTableFieldsIdMap,dsTableFieldsDatasetNameMap);
            } else {
                try {
                    List<AppCoreDatasourceVO> appCoreDatasourceVO = appData.getDatasourceInfo();
                    //  app 数据源 excel 表名映射
                    appCoreDatasourceVO.forEach(datasourceOld -> {
                        if (datasourceOld.getSystemDatasourceId() == null) {
                            throwAppImportMappingMissing("数据源", datasourceOld.getName());
                        }
                        newDatasourceId.add(datasourceOld.getSystemDatasourceId());
                        // Excel 数据表明映射
                        if (StringUtils.isNotEmpty(datasourceOld.getConfiguration())) {
                            if (datasourceOld.getType().equals(DatasourceConfiguration.DatasourceType.API.name())) {
                                CrestException.throwException(Translator.get("i18n_app_error_no_api"));
                            } else if (datasourceOld.getType().equals(DatasourceConfiguration.DatasourceType.Excel.name()) || datasourceOld.getType().equals(DatasourceConfiguration.DatasourceType.ExcelRemote.name())) {
                                dsTableNamesMap.put(datasourceOld.getId(), ExcelUtils.getTableNamesMap(datasourceOld.getType(), datasourceOld.getConfiguration()));
                            } else if (datasourceOld.getType().contains(DatasourceConfiguration.DatasourceType.API.name())) {
                                dsTableNamesMap.put(datasourceOld.getId(), (Map<String, String>) datasourceServer.invokeMethod(datasourceOld.getType(), "getTableNamesMap", String.class, datasourceOld.getConfiguration()));
                            }
                        }
                    });

                    List<CoreDatasource> systemDatasource = coreDatasourceMapper.selectBatchIds(newDatasourceId);
                    systemDatasource.forEach(datasourceNew -> {
                        // Excel 数据表明映射
                        if (StringUtils.isNotEmpty(datasourceNew.getConfiguration())) {
                            if (datasourceNew.getType().equals(DatasourceConfiguration.DatasourceType.Excel.name()) || datasourceNew.getType().equals(DatasourceConfiguration.DatasourceType.ExcelRemote.name())) {
                                dsTableNamesMap.put(datasourceNew.getId(), ExcelUtils.getTableNamesMap(datasourceNew.getType(), datasourceNew.getConfiguration()));
                            } else if (datasourceNew.getType().contains(DatasourceConfiguration.DatasourceType.API.name())) {
                                dsTableNamesMap.put(datasourceNew.getId(), (Map<String, String>) datasourceServer.invokeMethod(datasourceNew.getType(), "getTableNamesMap", String.class, datasourceNew.getConfiguration()));
                            }
                        }
                    });
                    datasourceIdMap.putAll(appData.getDatasourceInfo().stream().collect(Collectors.toMap(AppCoreDatasourceVO::getId, AppCoreDatasourceVO::getSystemDatasourceId)));
                    Map<String, String> datasourceTableNameMap = buildDatasourceTableNameMap(appData.getDatasourceInfo(), dsTableNamesMap, datasourceIdMap);
                    Long datasetFolderPid = request.getDatasetFolderPid();
                    String datasetFolderName = request.getDatasetFolderName();
                    //新建数据集分组
                    DatasetGroupInfoDTO datasetFolderNewRequest = new DatasetGroupInfoDTO();
                    datasetFolderNewRequest.setName(datasetFolderName);
                    datasetFolderNewRequest.setNodeType("folder");
                    datasetFolderNewRequest.setPid(datasetFolderPid);
                    DatasetGroupInfoDTO datasetFolderNew = datasetGroupManage.save(datasetFolderNewRequest, false, false);
                    Long datasetFolderNewId = datasetFolderNew.getId();
                    //新建数据集
                    appData.getDatasetGroupsInfo().forEach(appDatasetGroup -> {
                        if ("dataset".equals(appDatasetGroup.getNodeType())) {
                            Long oldId = appDatasetGroup.getId();
                            Long newId = IDUtils.snowID();
                            DatasetGroupInfoDTO datasetNewRequest = new DatasetGroupInfoDTO();
                            BeanUtils.copyBean(datasetNewRequest, appDatasetGroup);
                            datasetNewRequest.setId(newId);
                            datasetNewRequest.setCreateBy(AuthUtils.getUser().getUserId() + "");
                            datasetNewRequest.setUpdateBy(AuthUtils.getUser().getUserId() + "");
                            datasetNewRequest.setCreateTime(time);
                            datasetNewRequest.setLastUpdateTime(time);
                            datasetNewRequest.setPid(datasetFolderNewId);
                            try {
                                newDsGroupInfo.add(datasetNewRequest);
                                dsGroupIdMap.put(oldId, newId);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                    });
                    // 新建数据集表
                    appData.getDatasetTablesInfo().forEach(appCoreDatasetTableVO -> {
                        Long oldId = appCoreDatasetTableVO.getId();
                        Long newId = IDUtils.snowID();
                        CoreDatasetTable datasetTable = new CoreDatasetTable();
                        BeanUtils.copyBean(datasetTable, appCoreDatasetTableVO);
                        datasetTable.setDatasetGroupId(dsGroupIdMap.get(datasetTable.getDatasetGroupId()));
                        datasetTable.setId(newId);
                        datasetTable.setDatasourceId(datasourceIdMap.get(datasetTable.getDatasourceId()));
                        datasetTable.setInfo(replaceDatasetTableSql(datasetTable.getInfo(), datasourceTableNameMap));
                        datasetTable.setTableName(replaceTableNames(datasetTable.getTableName(), datasourceTableNameMap));
                        coreDatasetTableMapper.insert(datasetTable);
                        dsTableIdMap.put(oldId, newId);

                    });
                    // 新建数据字段
                    appData.getDatasetTableFieldsInfo().forEach(appDsTableFields -> {
                        Long oldId = appDsTableFields.getId();
                        Long newId = IDUtils.snowID();
                        CoreDatasetTableField dsDsField = new CoreDatasetTableField();
                        BeanUtils.copyBean(dsDsField, appDsTableFields);
                        dsDsField.setDatasetGroupId(dsGroupIdMap.get(dsDsField.getDatasetGroupId()));
                        dsDsField.setDatasetTableId(dsTableIdMap.get(dsDsField.getDatasetTableId()));
                        dsDsField.setDatasourceId(datasourceIdMap.get(dsDsField.getDatasourceId()));
                        dsDsField.setId(newId);
                        dsTableFieldsList.add(dsDsField);
                        dsTableFieldsIdMap.put(oldId, newId);
                    });

                    // dsTableFields 中存在计算字段在OriginName中 也需要替换
                    dsTableFieldsList.forEach(dsTableFields -> {
                        dsTableFieldsIdMap.forEach((key, value) -> {
                            dsTableFields.setOriginName(dsTableFields.getOriginName().replace(key.toString(), value.toString()));
                        });
                        coreDatasetTableFieldMapper.insert(dsTableFields);
                    });

                    List<String> dsGroupNameSave = new ArrayList<>();
                    // 持久化数据集
                    newDsGroupInfo.forEach(dsGroup -> {
                        dsGroup.setInfo(replaceMappedIdsPreservingDatasetSql(dsGroup.getInfo(), dsTableIdMap));
                        dsGroup.setInfo(replaceMappedIdsPreservingDatasetSql(dsGroup.getInfo(), dsTableFieldsIdMap));
                        dsGroup.setInfo(replaceMappedIdsPreservingDatasetSql(dsGroup.getInfo(), datasourceIdMap));
                        dsGroup.setInfo(replaceDatasetGroupSql(dsGroup.getInfo(), datasourceTableNameMap));
                        dsGroup.setUnionSql(replaceTableNames(dsGroup.getUnionSql(), datasourceTableNameMap));
                        if (dsGroupNameSave.contains(dsGroup.getName())) {
                            dsGroup.setName(dsGroup.getName() + "-" + UUID.randomUUID().toString());
                        }
                        dsGroupNameSave.add(dsGroup.getName());
                        if (dsGroup.getIsCross() == null) {
                            if (dsGroup.getUnion() == null) {
                                dsGroup.setUnion(JsonUtil.parseList(dsGroup.getInfo(), new TypeReference<>() {
                                }));
                            }
                            datasetSQLManage.mergeDatasetCrossDefault(dsGroup);
                        }
                        datasetGroupManage.innerSave(dsGroup);
                    });

                } catch (CrestException e) {
                    throw e;
                } catch (Exception e) {
                    LogUtil.error("应用导入保存失败", e);
                    CrestException.throwException(e);
                }
            }

            // 更换主数据内容
            AtomicReference<String> componentDataStr = new AtomicReference<>(request.getComponentData());
            replaceMappedIds(componentDataStr, dsGroupIdMap, "数据集");
            replaceMappedIds(componentDataStr, dsTableIdMap, "数据表");
            replaceMappedIds(componentDataStr, dsTableFieldsIdMap, "字段");
            datasourceIdMap.forEach((key, value) -> {
                replaceMappedId(componentDataStr, key, value, "数据源");
                //表名映射更新
                Map<String, String> appDsTableNamesMap = dsTableNamesMap.get(key);
                Map<String, String> systemDsTableNamesMap = dsTableNamesMap.get(value);
                if (MapUtils.isNotEmpty(appDsTableNamesMap) && MapUtils.isNotEmpty(systemDsTableNamesMap)) {
                    appDsTableNamesMap.forEach((keyName, valueName) -> {
                        if (StringUtils.isNotEmpty(systemDsTableNamesMap.get(keyName))) {
                            componentDataStr.set(componentDataStr.get().replace(key.toString(), value.toString()));
                        }
                    });
                }

            });
            request.setComponentData(componentDataStr.get());
        }
        DataVisualizationInfo visualizationInfo = new DataVisualizationInfo();
        BeanUtils.copyBean(visualizationInfo, request);
        visualizationInfo.setNodeType(request.getNodeType() == null ? DataVisualizationConstants.NODE_TYPE.LEAF : request.getNodeType());
        if (request.getSelfWatermarkStatus() != null && request.getSelfWatermarkStatus()) {
            visualizationInfo.setSelfWatermarkStatus(1);
        } else {
            visualizationInfo.setSelfWatermarkStatus(0);
        }
        if (DataVisualizationConstants.RESOURCE_OPT_TYPE.COPY.equals(request.getOptType())) {
            // 复制更新 新建权限插入
            visualizationInfoMapper.deleteById(request.getId());
            snapshotMapper.deleteById(request.getId());
            visualizationInfo.setNodeType(DataVisualizationConstants.NODE_TYPE.LEAF);
        }
        // 文件夹走默认发布 非文件夹默认未发布
        visualizationInfo.setStatus(DataVisualizationConstants.NODE_TYPE.FOLDER.equals(visualizationInfo.getNodeType()) ? CommonConstants.DV_STATUS.PUBLISHED : CommonConstants.DV_STATUS.UNPUBLISHED);
        Long newDvId = coreVisualizationManage.innerSave(visualizationInfo);
        request.setId(newDvId);
        // 还原ID信息
        Map<Long, ChartViewDTO> canvasViews = request.getCanvasViewInfo();
        if (isAppSave) {
            Map<Long, String> canvasViewsStr = VisualizationUtils.viewTransToStr(canvasViews);
            canvasViewsStr.forEach((viewId, viewInfoStr) -> {
                AtomicReference<String> mutableViewInfoStr = new AtomicReference<>(viewInfoStr);
                replaceMappedIds(mutableViewInfoStr, datasourceIdMap, "数据源");
                replaceMappedIds(mutableViewInfoStr, dsTableIdMap, "数据表");
                replaceMappedIds(mutableViewInfoStr, dsTableFieldsIdMap, "字段");
                replaceMappedIds(mutableViewInfoStr, dsGroupIdMap, "数据集");
                dsTableFieldsDatasetNameMap.forEach((key, value) -> {
                    mutableViewInfoStr.set(mutableViewInfoStr.get().replace(key, value));
                });
                canvasViewsStr.put(viewId, mutableViewInfoStr.get());
            });
            canvasViews = VisualizationUtils.viewTransToObj(canvasViewsStr);
            canvasViews.forEach((key, viewInfo) -> {
                viewInfo.setId(key);
                viewInfo.setDataFrom("dataset");
                if (viewInfo.getTableId() == null) {
                    viewInfo.setTableId(viewInfo.getSourceTableId());
                }
            });
            Map<String, String> viewIdMap = appData.getViewIdMap();
            // visualization_linkage
            appData.getLinkages().forEach(visualizationLinkageVO -> {
                Long oldId = visualizationLinkageVO.getId();
                Long newId = IDUtils.snowID();
                SnapshotVisualizationLinkage visualizationLinkage = new SnapshotVisualizationLinkage();
                BeanUtils.copyBean(visualizationLinkage, visualizationLinkageVO);
                visualizationLinkage.setDvId(newDvId);
                visualizationLinkage.setId(newId);
                linkageIdMap.put(oldId, newId);
                snapshotVisualizationLinkageMapper.insert(visualizationLinkage);
            });

            // visualization_linkage_field
            appData.getLinkageFields().forEach(visualizationLinkageFieldVO -> {
                Long oldId = visualizationLinkageFieldVO.getId();
                Long newId = IDUtils.snowID();
                SnapshotVisualizationLinkageField visualizationLinkageField = new SnapshotVisualizationLinkageField();
                BeanUtils.copyBean(visualizationLinkageField, visualizationLinkageFieldVO);
                visualizationLinkageField.setId(newId);
                visualizationLinkageField.setLinkageId(linkageIdMap.get(visualizationLinkageField.getLinkageId()));
                visualizationLinkageField.setSourceField(dsTableFieldsIdMap.get(visualizationLinkageField.getSourceField()));
                visualizationLinkageField.setTargetField(dsTableFieldsIdMap.get(visualizationLinkageField.getTargetField()));
                linkageFieldIdMap.put(oldId, newId);
                snapshotVisualizationLinkageFieldMapper.insert(visualizationLinkageField);
            });

            // visualization_link_jump
            appData.getLinkJumps().forEach(visualizationLinkJumpVO -> {
                Long oldId = visualizationLinkJumpVO.getId();
                Long newId = IDUtils.snowID();
                SnapshotVisualizationLinkJump visualizationLinkJump = new SnapshotVisualizationLinkJump();
                BeanUtils.copyBean(visualizationLinkJump, visualizationLinkJumpVO);
                visualizationLinkJump.setId(newId);
                visualizationLinkJump.setSourceDvId(newDvId);
                visualizationLinkJump.setSourceViewId(mappedLong(viewIdMap, visualizationLinkJump.getSourceViewId()));
                linkJumpIdMap.put(oldId, newId);
                snapshotVisualizationLinkJumpMapper.insert(visualizationLinkJump);
            });

            saveAppLinkJumpInfos(appData, linkJumpIdMap, linkJumpInfoIdMap, dsTableFieldsIdMap, dsTableFieldsDatasetNameMap, viewIdMap);
        }
        //保存图表信息
        chartDataManage.saveChartViewFromVisualization(request.getComponentData(), newDvId, canvasViews);
        return newDvId.toString();
    }

    private void replaceMappedIds(AtomicReference<String> content, Map<Long, Long> mapping, String resourceName) {
        mapping.forEach((key, value) -> replaceMappedId(content, key, value, resourceName));
    }

    private String replaceMappedIdsPreservingDatasetSql(String content, Map<Long, Long> mapping) {
        if (StringUtils.isBlank(content) || MapUtils.isEmpty(mapping)) {
            return content;
        }
        Map<String, String> encodedSqlByToken = new LinkedHashMap<>();
        String maskedContent = maskDatasetGroupEncodedSql(content, encodedSqlByToken);
        AtomicReference<String> mappedContent = new AtomicReference<>(maskedContent);
        mapping.forEach((key, value) -> mappedContent.set(mappedContent.get().replace(key.toString(), value.toString())));
        String result = mappedContent.get();
        for (Map.Entry<String, String> entry : encodedSqlByToken.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String maskDatasetGroupEncodedSql(String content, Map<String, String> encodedSqlByToken) {
        try {
            List<UnionDTO> unionDTOList = JsonUtil.parseList(content, new TypeReference<>() {});
            for (UnionDTO unionDTO : unionDTOList) {
                DatasetTableDTO tableDTO = unionDTO.getCurrentDs();
                if (tableDTO == null || StringUtils.isBlank(tableDTO.getInfo())) {
                    continue;
                }
                DatasetTableInfoDTO infoDTO = JsonUtil.parseObject(tableDTO.getInfo(), DatasetTableInfoDTO.class);
                if (infoDTO == null || StringUtils.isBlank(infoDTO.getSql())) {
                    continue;
                }
                String token = "__CREST_ENCODED_SQL_" + encodedSqlByToken.size() + "__";
                encodedSqlByToken.put(token, infoDTO.getSql());
                infoDTO.setSql(token);
                tableDTO.setInfo(Objects.requireNonNull(JsonUtil.toJSONString(infoDTO)).toString());
            }
            return Objects.requireNonNull(JsonUtil.toJSONString(unionDTOList)).toString();
        } catch (Exception e) {
            return content;
        }
    }

    private void replaceMappedId(AtomicReference<String> content, Long sourceId, Long targetId, String resourceName) {
        if (sourceId == null || targetId == null) {
            throwAppImportMappingMissing(resourceName, null);
        }
        content.set(content.get().replace(sourceId.toString(), targetId.toString()));
    }

    private void throwAppImportMappingMissing(String resourceName, String name) {
        String suffix = StringUtils.isBlank(name) ? "" : "：" + name;
        CrestException.throwException(
                ResultCode.PARAM_IS_INVALID.code(),
                "导入应用未完成" + resourceName + "匹配，请先选择目标" + resourceName + suffix
        );
    }

    void saveAppLinkJumpInfos(VisualizationExport2AppVO appData,
                              Map<Long, Long> linkJumpIdMap,
                              Map<Long, Long> linkJumpInfoIdMap,
                              Map<Long, Long> dsTableFieldsIdMap,
                              Map<String, String> dsTableFieldsDatasetNameMap,
                              Map<String, String> viewIdMap) {
        Optional.ofNullable(appData.getLinkJumpInfos()).orElseGet(ArrayList::new).forEach(visualizationLinkJumpInfoVO -> {
            Long oldId = visualizationLinkJumpInfoVO.getId();
            Long newId = IDUtils.snowID();
            SnapshotVisualizationLinkJumpInfo visualizationLinkJumpInfo = new SnapshotVisualizationLinkJumpInfo();
            BeanUtils.copyBean(visualizationLinkJumpInfo, visualizationLinkJumpInfoVO);
            visualizationLinkJumpInfo.setId(newId);
            visualizationLinkJumpInfo.setLinkJumpId(mappedLong(linkJumpIdMap, visualizationLinkJumpInfo.getLinkJumpId()));
            visualizationLinkJumpInfo.setSourceFieldId(mappedLong(dsTableFieldsIdMap, visualizationLinkJumpInfo.getSourceFieldId()));
            linkJumpInfoIdMap.put(oldId, newId);
            if ("outer".equals(visualizationLinkJumpInfo.getLinkType())) {
                visualizationLinkJumpInfo.setContent(mappedText(visualizationLinkJumpInfo.getContent(), dsTableFieldsIdMap, dsTableFieldsDatasetNameMap));
            }
            snapshotVisualizationLinkJumpInfoMapper.insert(visualizationLinkJumpInfo);
        });

        Optional.ofNullable(appData.getLinkJumpTargetInfos()).orElseGet(ArrayList::new).forEach(visualizationLinkJumpTargetViewInfoVO -> {
            SnapshotVisualizationLinkJumpTargetViewInfo targetViewInfo = new SnapshotVisualizationLinkJumpTargetViewInfo();
            BeanUtils.copyBean(targetViewInfo, visualizationLinkJumpTargetViewInfoVO);
            targetViewInfo.setTargetId(IDUtils.snowID());
            targetViewInfo.setLinkJumpInfoId(mappedLong(linkJumpInfoIdMap, targetViewInfo.getLinkJumpInfoId()));
            targetViewInfo.setSourceFieldActiveId(mappedLong(dsTableFieldsIdMap, targetViewInfo.getSourceFieldActiveId()));
            targetViewInfo.setTargetFieldId(mappedStringId(dsTableFieldsIdMap, targetViewInfo.getTargetFieldId()));
            targetViewInfo.setTargetViewId(mappedStringId(viewIdMap, targetViewInfo.getTargetViewId()));
            snapshotVisualizationLinkJumpTargetViewInfoMapper.insert(targetViewInfo);
        });
    }

    private Long mappedLong(Map<?, ?> mapping, Long value) {
        if (value == null || MapUtils.isEmpty(mapping)) {
            return value;
        }
        Object mapped = mapping.get(value);
        if (mapped == null) {
            mapped = mapping.get(value.toString());
        }
        if (mapped == null) {
            return value;
        }
        try {
            return Long.valueOf(mapped.toString());
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String mappedStringId(Map<?, ?> mapping, String value) {
        if (StringUtils.isBlank(value) || MapUtils.isEmpty(mapping)) {
            return value;
        }
        Object mapped = mapping.get(value);
        if (mapped != null) {
            return mapped.toString();
        }
        try {
            mapped = mapping.get(Long.parseLong(value));
            return mapped == null ? value : mapped.toString();
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String mappedText(String content, Map<Long, Long> fieldIdMap, Map<String, String> datasetNameMap) {
        if (StringUtils.isEmpty(content)) {
            return content;
        }
        AtomicReference<String> mappedContent = new AtomicReference<>(content);
        fieldIdMap.forEach((key, value) -> mappedContent.set(mappedContent.get().replace(key.toString(), value.toString())));
        datasetNameMap.forEach((key, value) -> mappedContent.set(mappedContent.get().replace(key, value)));
        return mappedContent.get();
    }

    private Map<String, String> buildDatasourceTableNameMap(List<AppCoreDatasourceVO> appDatasources,
                                                            Map<Long, Map<String, String>> datasourceTableNames,
                                                            Map<Long, Long> datasourceIdMap) {
        Map<String, String> result = new HashMap<>();
        Optional.ofNullable(appDatasources).orElseGet(ArrayList::new).forEach(sourceDatasource -> {
            Long targetDatasourceId = datasourceIdMap.get(sourceDatasource.getId());
            Map<String, String> sourceTables = datasourceTableNames.get(sourceDatasource.getId());
            Map<String, String> targetTables = datasourceTableNames.get(targetDatasourceId);
            if (MapUtils.isEmpty(sourceTables) || MapUtils.isEmpty(targetTables)) {
                return;
            }
            boolean singleTableMapping = sourceTables.size() == 1 && targetTables.size() == 1;
            sourceTables.forEach((sourceTable, sourceRuntimeTable) -> {
                String targetRuntimeTable = singleTableMapping
                        ? targetTables.values().iterator().next()
                        : targetTables.get(sourceTable);
                if (StringUtils.isBlank(targetRuntimeTable)) {
                    targetRuntimeTable = "excel_can_not_find";
                }
                result.put(sourceTable, targetRuntimeTable);
                result.put(sourceRuntimeTable, targetRuntimeTable);
            });
        });
        return result;
    }

    private String replaceDatasetGroupSql(String info, Map<String, String> tableNameMap) {
        if (StringUtils.isBlank(info) || MapUtils.isEmpty(tableNameMap)) {
            return info;
        }
        try {
            List<UnionDTO> unionDTOList = JsonUtil.parseList(info, new TypeReference<>() {});
            unionDTOList.forEach(unionDTO -> {
                DatasetTableDTO tableDTO = unionDTO.getCurrentDs();
                if (tableDTO == null) {
                    return;
                }
                tableDTO.setTableName(replaceTableNames(tableDTO.getTableName(), tableNameMap));
                tableDTO.setInfo(replaceDatasetTableSql(tableDTO.getInfo(), tableNameMap));
            });
            return Objects.requireNonNull(JsonUtil.toJSONString(unionDTOList)).toString();
        } catch (Exception e) {
            return replaceTableNames(info, tableNameMap);
        }
    }

    private String replaceDatasetTableSql(String info, Map<String, String> tableNameMap) {
        if (StringUtils.isBlank(info) || MapUtils.isEmpty(tableNameMap)) {
            return info;
        }
        try {
            DatasetTableInfoDTO infoDTO = JsonUtil.parseObject(info, DatasetTableInfoDTO.class);
            if (infoDTO == null) {
                return replaceTableNames(info, tableNameMap);
            }
            infoDTO.setTable(replaceTableNames(infoDTO.getTable(), tableNameMap));
            if (StringUtils.isNotBlank(infoDTO.getSql())) {
                String sql = new String(Base64.getDecoder().decode(infoDTO.getSql()), StandardCharsets.UTF_8);
                infoDTO.setSql(Base64.getEncoder().encodeToString(replaceTableNames(sql, tableNameMap).getBytes(StandardCharsets.UTF_8)));
            }
            return Objects.requireNonNull(JsonUtil.toJSONString(infoDTO)).toString();
        } catch (Exception e) {
            return replaceTableNames(info, tableNameMap);
        }
    }

    private String replaceTableNames(String content, Map<String, String> tableNameMap) {
        if (StringUtils.isBlank(content) || MapUtils.isEmpty(tableNameMap)) {
            return content;
        }
        String mappedContent = content;
        List<Map.Entry<String, String>> mappings = tableNameMap.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
                .sorted((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()))
                .toList();
        for (Map.Entry<String, String> mapping : mappings) {
            mappedContent = mappedContent.replace(mapping.getKey(), mapping.getValue());
        }
        return mappedContent;
    }

    @Override
    public String appCanvasNameCheck(@RequestBody DataVisualizationBaseRequest request) throws Exception {
        Long datasetFolderPid = request.getDatasetFolderPid();
        String datasetFolderName = request.getDatasetFolderName();
        QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", datasetFolderName);
        queryWrapper.eq("pid", datasetFolderPid);
        queryWrapper.eq("node_type", DataVisualizationConstants.NODE_TYPE.FOLDER);
        if (coreDatasetGroupMapper.exists(queryWrapper)) {
            return "repeat";
        } else {
            return "success";
        }
    }

    @Override
    public String checkCanvasChange(@RequestBody DataVisualizationBaseRequest request) {
        Long dvId = request.getId();
        if (dvId == null) {
            CrestException.throwException("ID can not be null");
        }
        // 内容ID校验
        QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("content_id", request.getContentId());
        queryWrapper.eq("id", dvId);
        if (!visualizationInfoMapper.exists(queryWrapper)) {
            return "Repeat";
        }
        return "Success";
    }

    @CrestAudit(id = "#p0.id", ot = LogOT.MODIFY, stExp = "#p0.type")
    @Override
    @Transactional
    public DataVisualizationVO updateCanvas(@RequestBody DataVisualizationBaseRequest request) {
        for (Map.Entry<Long, ChartViewDTO> ele : request.getCanvasViewInfo().entrySet()) {
            DatasetUtils.viewDecode(ele.getValue());
        }
        Long dvId = request.getId();
        if (dvId == null) {
            CrestException.throwException("ID can not be null");
        }
        DataVisualizationInfo visualizationInfo = new DataVisualizationInfo();
        BeanUtils.copyBean(visualizationInfo, request);
        if (request.getSelfWatermarkStatus() != null && request.getSelfWatermarkStatus()) {
            visualizationInfo.setSelfWatermarkStatus(1);
        } else {
            visualizationInfo.setSelfWatermarkStatus(0);
        }

        // 检查当前节点的pid是否一致如果不一致 需要调用move 接口(预存 可能会出现pid =-1的情况)
        if (request.getPid() != -1) {
            QueryWrapper<DataVisualizationInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("pid", request.getPid());
            queryWrapper.eq("id", dvId);
            if (!visualizationInfoMapper.exists(queryWrapper)) {
                request.setMoveFromUpdate(true);
                coreVisualizationManage.move(request);
            }
        }
        // 状态修改统一为后端操作：历史状态检查 如果 状态为 0（未发布） 或者 2（已发布未保存）则状态不变
        // 如果当前状态为 1 则状态修改为  2（已发布未保存）
        Integer curStatus = extDataVisualizationMapper.findDvInfoStats(dvId);
        visualizationInfo.setStatus(curStatus == 1 ? CommonConstants.DV_STATUS.SAVED_UNPUBLISHED : curStatus);
        coreVisualizationManage.innerEdit(visualizationInfo);
        //保存图表信息
        chartDataManage.saveChartViewFromVisualization(request.getComponentData(), dvId, request.getCanvasViewInfo());
        return new DataVisualizationVO(visualizationInfo.getStatus());
    }

    @Override
    @Transactional
    public void updatePublishStatus(@RequestBody DataVisualizationBaseRequest request) {
        /**
         * 如果当前传入状态是1（已发布），则原始状态0（未发布）-》1（已发布）；2（已保存未发布）-》1（已发布）
         * 统一处理为1.删除主表数据，2.将镜像表数据统一copy到主表 不删除镜像数据（发布状态后镜像数据和主表数据是保持一致的）
         * 其他状态仅更新主表和镜像表状态
         * */
        Long dvId = request.getId();
        DataVisualizationInfo visualizationInfo = new DataVisualizationInfo();
        visualizationInfo.setMobileLayout(request.getMobileLayout());
        visualizationInfo.setId(dvId);
        visualizationInfo.setName(request.getName());
        visualizationInfo.setStatus(request.getStatus());
        coreVisualizationManage.innerEdit(visualizationInfo);
        if (CommonConstants.DV_STATUS.PUBLISHED == request.getStatus()) {
            List<Long> viewIds = this.getEnabledViewIds(dvId, CommonConstants.RESOURCE_TABLE.SNAPSHOT);
            extDataVisualizationMapper.deleteUselessViewsBatchSnapshot(viewIds, dvId);
            coreVisualizationManage.removeDvCore(dvId);
            coreVisualizationManage.dvRestore(dvId);
            chartViewManege.publishThreshold(dvId, request.getActiveViewIds());
        } else if (CommonConstants.DV_STATUS.UNPUBLISHED == request.getStatus()) {
            chartViewManege.publishThreshold(dvId, request.getActiveViewIds());
        }
    }

    @Override
    public void recoverToPublished(@RequestBody DataVisualizationBaseRequest request) {
        DataVisualizationInfo current = visualizationInfoMapper.selectById(request.getId());
        coreVisualizationManage.requireVisualizationManage(current);
        coreVisualizationManage.dvSnapshotRecover(request.getId());
        DataVisualizationInfo visualizationInfo = new DataVisualizationInfo();
        visualizationInfo.setId(request.getId());
        visualizationInfo.setName(request.getName());
        visualizationInfo.setStatus(CommonConstants.DV_STATUS.PUBLISHED);
        coreVisualizationManage.innerEdit(visualizationInfo);
    }

    /**
     * @Description: 更新基础信息；
     * 为什么单独接口：1.基础信息更新频繁数据且数据载量较小；2.防止出现更新过多信息的情况，造成图表的误删等操作
     */
    @CrestAudit(id = "#p0.id", ot = LogOT.MODIFY, stExp = "#p0.type")
    @Override
    @Transactional
    public void updateBase(@RequestBody DataVisualizationBaseRequest request) {
        if (request.getId() != null) {
            coreVisualizationManage.innerEdit(BeanUtils.copyBean(new DataVisualizationInfo(), request));
        } else {
            CrestException.throwException("Id can not be null");
        }
    }

    /**
     * @Description: 逻辑删除可视化信息；将delete_flag 置为0
     */
    @CrestAudit(id = "#p0", ot = LogOT.DELETE, stExp = "#p1")
    @Transactional
    @Override
    public void deleteLogic(Long dvId, String busiFlag) {
        coreVisualizationManage.delete(dvId);
    }

    private void resourceTreeTypeAdaptor(List<BusiNodeVO> tree, String type) {
        if (!CollectionUtils.isEmpty(tree)) {
            tree.forEach(busiNodeVO -> {
                busiNodeVO.setType(type);
                resourceTreeTypeAdaptor(busiNodeVO.getChildren(), type);
            });
        }
    }

    @CrestAudit(ot = LogOT.READ, st = LogST.PANEL)
    @Override
    public List<BusiNodeVO> tree(@RequestBody BusiNodeRequest request) {
        if (StringUtils.isEmpty(request.getResourceTable())) {
            request.setResourceTable(CommonConstants.RESOURCE_TABLE.SNAPSHOT);
        }
        String busiFlag = request.getBusiFlag();
        if (Strings.CS.equals(busiFlag, "dashboard-dataV")) {
            BusiNodeRequest requestDv = new BusiNodeRequest();
            BeanUtils.copyBean(requestDv, request);
            requestDv.setBusiFlag("dashboard");
            List<BusiNodeVO> dashboardResult = coreVisualizationManage.tree(requestDv);
            requestDv.setBusiFlag("dataV");
            List<BusiNodeVO> dataVResult = coreVisualizationManage.tree(requestDv);
            List<BusiNodeVO> result = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dashboardResult)) {
                resourceTreeTypeAdaptor(dashboardResult, "dashboard");
                BusiNodeVO dashboardResultParent = new BusiNodeVO();
                dashboardResultParent.setName(Translator.get("i18n_menu.panel"));
                dashboardResultParent.setId(-101L);
                if (dashboardResult.get(0).getId() == 0) {
                    dashboardResultParent.setChildren(dashboardResult.get(0).getChildren());
                } else {
                    dashboardResultParent.setChildren(dashboardResult);
                }
                result.add(dashboardResultParent);
            }
            if (!CollectionUtils.isEmpty(dataVResult)) {
                resourceTreeTypeAdaptor(dataVResult, "dataV");
                BusiNodeVO dataVResultParent = new BusiNodeVO();
                dataVResultParent.setName(Translator.get("i18n_menu.screen"));
                dataVResultParent.setId(-102L);
                if (dataVResult.get(0).getId() == 0) {
                    dataVResultParent.setChildren(dataVResult.get(0).getChildren());
                } else {
                    dataVResultParent.setChildren(dataVResult);
                }
                result.add(dataVResultParent);
            }
            return result;
        } else {
            return coreVisualizationManage.tree(request);
        }
    }

    @Override
    public Map<String, List<BusiNodeVO>> interactiveTree(@RequestBody Map<String, BusiNodeRequest> requestMap) {
        return coreBusiManage.interactiveTree(requestMap);
    }

    @CrestAudit(id = "#p0.id", pid = "#p0.pid", ot = LogOT.MODIFY, stExp = "#p0.type")
    @Transactional
    @Override
    public void move(@RequestBody DataVisualizationBaseRequest request) {
        coreVisualizationManage.move(request);
    }

    @Override
    public List<VisualizationResourceVO> findRecent(@RequestBody VisualizationWorkbranchQueryRequest request) {
        request.setQueryFrom("recent");
        IPage<VisualizationResourceVO> result = coreVisualizationManage.query(1, 20, request);
        return result.getRecords();
    }

    /**
     * @Description: 复制仪表板
     * 复制步骤 1.复制基础可视化数据；2.复制图表数据；3.附加数据（包括联动信息，跳转信息，外部参数信息等仪表板附加信息）
     */
    @Transactional
    @Override
    public String copy(@RequestBody DataVisualizationBaseRequest request) {
        Long sourceDvId = request.getId(); //源仪表板ID
        Long newDvId = IDUtils.snowID(); //目标仪表板ID
        Long copyId = IDUtils.snowID() / 100; // 本次复制执行ID
        // 复制仪表板
        DataVisualizationInfo newDv = visualizationInfoMapper.selectById(sourceDvId);
        coreVisualizationManage.requireVisualizationManage(newDv);
        if (request.getPid() != null && request.getPid() > 0) {
            DataVisualizationInfo parent = visualizationInfoMapper.selectById(request.getPid());
            if (parent != null) {
                coreVisualizationManage.requireVisualizationManage(parent);
            }
        }
        newDv.setName(request.getName());
        newDv.setId(newDvId);
        newDv.setPid(request.getPid());
        newDv.setCreateTime(System.currentTimeMillis());
        // 复制图表 chart_view
        extDataVisualizationMapper.viewCopyWithDv(sourceDvId, newDvId, copyId, CommonConstants.RESOURCE_TABLE.CORE);
        extDataVisualizationMapper.viewCopyWithDv(sourceDvId, newDvId, copyId, CommonConstants.RESOURCE_TABLE.SNAPSHOT);
        List<CoreChartView> viewList = extDataVisualizationMapper.findViewInfoByCopyId(copyId);
        if (!CollectionUtils.isEmpty(viewList)) {
            String componentData = newDv.getComponentData();
            // componentData viewId 数据  并保存
            for (CoreChartView viewInfo : viewList) {
                componentData = componentData.replace(String.valueOf(viewInfo.getCopyFrom()), String.valueOf(viewInfo.getId()));
            }
            newDv.setComponentData(componentData);
        }
        // 复制图表联动信息
        extDataVisualizationMapper.copyLinkage(copyId);
        extDataVisualizationMapper.copyLinkageField(copyId);
        // 复制图表跳转信息
        extDataVisualizationMapper.copyLinkJump(copyId);
        extDataVisualizationMapper.copyLinkJumpInfo(copyId);
        extDataVisualizationMapper.copyLinkJumpTargetInfo(copyId);
        DataVisualizationInfo visualizationInfoTarget = new DataVisualizationInfo();
        BeanUtils.copyBean(visualizationInfoTarget, newDv);
        visualizationInfoTarget.setPid(-1L);
        coreVisualizationManage.preInnerSave(visualizationInfoTarget);
        return String.valueOf(newDvId);
    }

    @Override
    public String findDvType(Long dvId) {
        String result = extDataVisualizationMapper.findDvType(dvId);
        if (StringUtils.isEmpty(result)) {
            CrestException.throwException(Translator.get("i18n_resource_not_exists"));
        }
        return result;
    }

    @Override
    public String updateCheckVersion(Long dvId) {
        DataVisualizationInfo current = visualizationInfoMapper.selectById(dvId);
        coreVisualizationManage.requireVisualizationManage(current);
        DataVisualizationInfo updateInfo = new DataVisualizationInfo();
        updateInfo.setId(dvId);
        updateInfo.setCheckVersion(crestVersion);
        visualizationInfoMapper.updateById(updateInfo);
        return "";
    }

    @Override
    public DataVisualizationVO decompression(@RequestBody DataVisualizationBaseRequest request) throws Exception {
        try {
            Long newDvId = IDUtils.snowID();
            String newFrom = request.getNewFrom();
            Map<String,String> viewIdsMap = new HashMap<>();
            String templateStyle = null;
            String templateData = null;
            String dynamicData = null;
            String staticResource = null;
            String appDataStr = null;
            String name = null;
            String dvType = null;
            Integer version = null;
            //内部模板新建
            if (DataVisualizationConstants.NEW_PANEL_FROM.NEW_INNER_TEMPLATE.equals(newFrom)) {
                VisualizationTemplate visualizationTemplate = templateMapper.selectById(request.getTemplateId());
                templateStyle = visualizationTemplate.getTemplateStyle();
                templateData = visualizationTemplate.getTemplateData();
                dynamicData = visualizationTemplate.getDynamicData();
                name = visualizationTemplate.getName();
                dvType = visualizationTemplate.getDvType();
                version = visualizationTemplate.getVersion();
                appDataStr = visualizationTemplate.getAppData();
                // 模板市场记录
                coreOptRecentManage.saveOpt(request.getTemplateId(), OptConstants.OPT_RESOURCE_TYPE.TEMPLATE, OptConstants.OPT_TYPE.NEW);
                VisualizationTemplate visualizationTemplateUpdate = new VisualizationTemplate();
                visualizationTemplateUpdate.setId(visualizationTemplate.getId());
                visualizationTemplateUpdate.setUseCount(visualizationTemplate.getUseCount() == null ? 0 : visualizationTemplate.getUseCount() + 1);
                templateMapper.updateById(visualizationTemplateUpdate);
            } else if (DataVisualizationConstants.NEW_PANEL_FROM.NEW_OUTER_TEMPLATE.equals(newFrom)) {
                templateStyle = request.getCanvasStyleData();
                templateData = request.getComponentData();
                dynamicData = request.getDynamicData();
                staticResource = request.getStaticResource();
                name = request.getName();
                dvType = request.getType();
                version = request.getVersion();
                if (request.getAppData() != null) {
                    appDataStr = Objects.requireNonNull(JsonUtil.toJSONString(request.getAppData())).toString();
                }
            } else if (DataVisualizationConstants.NEW_PANEL_FROM.NEW_MARKET_TEMPLATE.equals(newFrom)) {
                TemplateManageFileDTO templateFileInfo = templateCenterManage.getTemplateFromMarketV2(request.getResourceName());
                if (templateFileInfo == null) {
                    CrestException.throwException("Can't find the template's info from market,please check");
                }
                templateStyle = templateFileInfo.getCanvasStyleData();
                templateData = templateFileInfo.getComponentData();
                dynamicData = templateFileInfo.getDynamicData();
                staticResource = templateFileInfo.getStaticResource();
                name = templateFileInfo.getName();
                dvType = templateFileInfo.getDvType();
                version = templateFileInfo.getVersion();
                appDataStr = templateFileInfo.getAppData();
                // 模板市场记录
                coreOptRecentManage.saveOpt(request.getResourceName(), OptConstants.OPT_RESOURCE_TYPE.TEMPLATE, OptConstants.OPT_TYPE.NEW);
            }
            if (StringUtils.isNotEmpty(appDataStr) && appDataStr.length() > 10) {
                try {
                    VisualizationExport2AppVO appDataFormat = JsonUtil.parseObject(appDataStr, VisualizationExport2AppVO.class);
                    String dvInfo = appDataFormat.getVisualizationInfo();
                    VisualizationBaseInfoVO baseInfoVO = JsonUtil.parseObject(dvInfo, VisualizationBaseInfoVO.class);
                    Long sourceDvId = baseInfoVO.getId();
                    appDataStr = appDataStr.replace(sourceDvId.toString(), newDvId.toString());
                } catch (Exception e) {
                    LogUtil.error(e);
                    appDataStr = null;
                }
            } else {
                appDataStr = null;
            }
            // 解析动态数据
            Map<String, String> dynamicDataMap = JsonUtil.parseObject(dynamicData, Map.class);
            List<ChartViewDTO> chartViews = new ArrayList<>();
            Map<Long, ChartViewDTO> canvasViewInfo = new HashMap<>();
            Map<Long, VisualizationTemplateExtendDataDTO> extendDataInfo = new HashMap<>();
            for (Map.Entry<String, String> entry : dynamicDataMap.entrySet()) {
                String originViewId = entry.getKey();
                Object viewInfo = entry.getValue();
                try {
                    // 旧模板图表过滤器适配
                    if (viewInfo instanceof Map && ((Map) viewInfo).get("customFilter") instanceof ArrayList) {
                        ((Map) viewInfo).put("customFilter", new HashMap<>());
                    }
                } catch (Exception e) {
                    LogUtil.error("History Adaptor Error", e);
                }
                String originViewData = JsonUtil.toJSONString(entry.getValue()).toString();
                ChartViewDTO chartView = JsonUtil.parseObject(originViewData, ChartViewDTO.class);
                if (chartView == null) {
                    continue;
                }
                Long newViewId = IDUtils.snowID();
                chartView.setId(newViewId);
                chartView.setSceneId(newDvId);
                chartView.setSourceTableId(chartView.getTableId());
                chartView.setTableId(null);

                chartView.setDataFrom(CommonConstants.VIEW_DATA_FROM.TEMPLATE);
                // 数据处理 1.替换viewId 2.加入模板view data数据
                VisualizationTemplateExtendDataDTO extendDataDTO = new VisualizationTemplateExtendDataDTO(newDvId, newViewId, originViewData);
                extendDataInfo.put(newViewId, extendDataDTO);
                templateData = replaceTemplateComponentViewId(templateData, originViewId, newViewId.toString());
                viewIdsMap.put(originViewId,Long.toString(newViewId));
                String currentAppDataStr = appDataStr;
                if (currentAppDataStr != null && !currentAppDataStr.isEmpty()) {
                    chartView.setTableId(chartView.getSourceTableId());
                    appDataStr = currentAppDataStr.replace(originViewId, newViewId.toString());
                }
                canvasViewInfo.put(chartView.getId(), chartView);
                //插入模板数据 此处预先插入减少数据交互量
                VisualizationTemplateExtendData extendData = new VisualizationTemplateExtendData();
                templateExtendDataMapper.insert(BeanUtils.copyBean(extendData, extendDataDTO));
            }
            request.setComponentData(templateData);
            request.setCanvasStyleData(templateStyle);
            //Store static resource into the server
            staticResourceServer.saveFilesToServe(staticResource);
            return new DataVisualizationVO(newDvId, name, dvType, version, templateStyle, templateData, appDataStr, canvasViewInfo, null,viewIdsMap);
        } catch (Exception e) {
            io.crest.utils.LogUtil.error(e.getMessage(), e);
            CrestException.throwException("解析错误");
            return null;
        }

    }

    private String replaceTemplateComponentViewId(String componentData, String originViewId, String newViewId) {
        if (StringUtils.isBlank(componentData)) {
            return componentData;
        }
        Object components = JsonUtil.parseObject(componentData, new TypeReference<Object>() {});
        if (components != null && replaceViewIdValue(components, originViewId, newViewId)) {
            return JsonUtil.toJSONString(components).toString();
        }
        return componentData
                .replace("\"id\":\"" + originViewId + "\"", "\"id\":\"" + newViewId + "\"")
                .replace("\"id\": \"" + originViewId + "\"", "\"id\":\"" + newViewId + "\"")
                .replace("\"id\":" + originViewId, "\"id\":\"" + newViewId + "\"")
                .replace("\"id\": " + originViewId, "\"id\":\"" + newViewId + "\"")
                .replace(originViewId, newViewId);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean replaceViewIdValue(Object value, String originViewId, String newViewId) {
        boolean changed = false;
        if (value instanceof Map map) {
            for (Object key : new ArrayList<>(map.keySet())) {
                Object item = map.get(key);
                if (originViewId.equals(String.valueOf(item))) {
                    map.put(key, newViewId);
                    changed = true;
                } else if (replaceViewIdValue(item, originViewId, newViewId)) {
                    changed = true;
                }
            }
        } else if (value instanceof List list) {
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (originViewId.equals(String.valueOf(item))) {
                    list.set(i, newViewId);
                    changed = true;
                } else if (replaceViewIdValue(item, originViewId, newViewId)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public DataVisualizationVO decompressionLocalFile(MultipartFile file) {
        try {
            DataVisualizationBaseRequest request = parseLocalTemplate(file);
            return decompression(request);
        } catch (CrestException e) {
            throw e;
        } catch (Exception e) {
            LogUtil.error("Import visualization template file error", e);
            CrestException.throwException("模板文件解析失败，请确认文件格式是否正确");
            return null;
        }
    }

    private DataVisualizationBaseRequest parseLocalTemplate(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            CrestException.throwException("请选择模板文件");
        }
        String fileName = StringUtils.defaultString(file.getOriginalFilename());
        FileUtils.validateUploadFilename(fileName);
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerFileName.endsWith(".det2") && !lowerFileName.endsWith(".det2app")) {
            CrestException.throwException("仅支持 DET2 或 DET2APP 模板文件");
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(content)) {
            CrestException.throwException("模板文件内容为空");
        }
        Map<String, Object> templateInfo = JsonUtil.parseObject(content, new TypeReference<>() {});
        if (MapUtils.isEmpty(templateInfo)) {
            CrestException.throwException("模板文件内容为空");
        }

        DataVisualizationBaseRequest request = new DataVisualizationBaseRequest();
        request.setNewFrom(DataVisualizationConstants.NEW_PANEL_FROM.NEW_OUTER_TEMPLATE);
        request.setName(requireTemplateValue(templateInfo, "name", "模板名称不能为空"));
        request.setType(requireTemplateValue(templateInfo, "dvType", "模板类型不能为空"));
        if (!Strings.CI.equalsAny(request.getType(), "dataV", "dashboard")) {
            CrestException.throwException("模板类型不正确");
        }
        request.setVersion(templateVersion(templateInfo.get("version")));
        request.setCanvasStyleData(requireTemplateValue(templateInfo, "canvasStyleData", "模板样式数据不能为空"));
        request.setComponentData(requireTemplateValue(templateInfo, "componentData", "模板组件数据不能为空"));
        request.setDynamicData(requireTemplateValue(templateInfo, "dynamicData", "模板图表数据不能为空"));
        request.setStaticResource(templateValue(templateInfo, "staticResource"));

        String appData = templateValue(templateInfo, "appData");
        if (StringUtils.isNotBlank(appData)) {
            request.setAppData(JsonUtil.parseObject(appData, VisualizationExport2AppVO.class));
        }
        return request;
    }

    private String requireTemplateValue(Map<String, Object> templateInfo, String key, String message) {
        String value = templateValue(templateInfo, key);
        if (StringUtils.isBlank(value)) {
            CrestException.throwException(message);
        }
        return value;
    }

    private String templateValue(Map<String, Object> templateInfo, String key) {
        Object value = templateInfo.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        return Objects.requireNonNull(JsonUtil.toJSONString(value)).toString();
    }

    private Integer templateVersion(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString();
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            CrestException.throwException("模板版本号不正确");
            return null;
        }
    }

    @Override
    public List<VisualizationViewTableDTO> detailList(Long dvId) {
        requireVisualizationRead(dvId);
        List<VisualizationViewTableDTO> result = extDataVisualizationMapper.getVisualizationViewDetails(dvId);
        SnapshotDataVisualizationInfo dvInfo = snapshotMapper.selectById(dvId);
        if (dvInfo != null && !CollectionUtils.isEmpty(result)) {
            String componentData = dvInfo.getComponentData();
            return result.stream().filter(item -> componentData.indexOf("\"id\":\"" + item.getId()) > 0).toList();
        } else {
            return result;
        }
    }

    @Override
    public VisualizationExport2AppVO export2AppCheck(@RequestBody VisualizationAppExportRequest appExportRequest) {
        validateAppExportScope(appExportRequest);
        List<Long> viewIds = appExportRequest.getViewIds();
        List<Long> dsIds = appExportRequest.getDsIds();
        Long dvId = appExportRequest.getDvId();
        List<AppCoreChartViewVO> chartViewVOInfo = null;
        List<AppCoreDatasetGroupVO> datasetGroupVOInfo = null;
        List<AppCoreDatasetTableVO> datasetTableVOInfo = null;
        List<AppCoreDatasetTableFieldVO> datasetTableFieldVOInfo = null;
        List<AppCoreDatasourceVO> datasourceVOInfo = null;
        List<AppCoreDatasourceTaskVO> datasourceTaskVOInfo = null;
        //获取所有视图信息
        if (!CollectionUtils.isEmpty(viewIds)) {
            chartViewVOInfo = findAppChartViews(viewIds);
        }
        if (!CollectionUtils.isEmpty(dsIds)) {
            datasetGroupVOInfo = appTemplateMapper.findAppDatasetGroupInfo(dsIds);
            datasetTableVOInfo = appTemplateMapper.findAppDatasetTableInfo(dsIds);
            datasetTableFieldVOInfo = appTemplateMapper.findAppDatasetTableFieldInfo(dsIds);
            datasourceVOInfo = appTemplateMapper.findAppDatasourceInfo(dsIds);
            datasourceTaskVOInfo = appTemplateMapper.findAppDatasourceTaskInfo(dsIds);
        }

        if (CollectionUtils.isEmpty(datasourceVOInfo)) {
            CrestException.throwException("当前不存在数据源无法导出");
        } else if (datasourceVOInfo.stream().anyMatch(datasource -> datasource.getType().contains(DatasourceConfiguration.DatasourceType.API.name()))) {
            CrestException.throwException(Translator.get("i18n_app_error_no_api"));
        }

        List<VisualizationLinkageVO> snapshotLinkageVOInfo = appTemplateMapper.findAppSnapshotLinkageInfo(dvId);
        List<VisualizationLinkageVO> linkageVOInfo = CollectionUtils.isEmpty(snapshotLinkageVOInfo)
                ? appTemplateMapper.findAppLinkageInfo(dvId)
                : snapshotLinkageVOInfo;
        List<VisualizationLinkageFieldVO> linkageFieldVOInfo = CollectionUtils.isEmpty(snapshotLinkageVOInfo)
                ? appTemplateMapper.findAppLinkageFieldInfo(dvId)
                : appTemplateMapper.findAppSnapshotLinkageFieldInfo(dvId);

        List<VisualizationLinkJumpVO> snapshotLinkJumpVOInfo = appTemplateMapper.findAppSnapshotLinkJumpInfo(dvId);
        List<VisualizationLinkJumpVO> linkJumpVOInfo = CollectionUtils.isEmpty(snapshotLinkJumpVOInfo)
                ? appTemplateMapper.findAppLinkJumpInfo(dvId)
                : snapshotLinkJumpVOInfo;
        List<VisualizationLinkJumpInfoVO> linkJumpInfoVOInfo = CollectionUtils.isEmpty(snapshotLinkJumpVOInfo)
                ? appTemplateMapper.findAppLinkJumpInfoInfo(dvId)
                : appTemplateMapper.findAppSnapshotLinkJumpInfoInfo(dvId);
        List<VisualizationLinkJumpTargetViewInfoVO> listJumpTargetViewInfoVO = CollectionUtils.isEmpty(snapshotLinkJumpVOInfo)
                ? appTemplateMapper.findAppLinkJumpTargetViewInfoInfo(dvId)
                : appTemplateMapper.findAppSnapshotLinkJumpTargetViewInfoInfo(dvId);

        return new VisualizationExport2AppVO(chartViewVOInfo, datasetGroupVOInfo, datasetTableVOInfo, datasetTableFieldVOInfo, datasourceVOInfo, datasourceTaskVOInfo, linkJumpVOInfo, linkJumpInfoVOInfo, listJumpTargetViewInfoVO, linkageVOInfo, linkageFieldVOInfo);
    }

    private List<AppCoreChartViewVO> findAppChartViews(List<Long> viewIds) {
        List<AppCoreChartViewVO> publishedViews = appTemplateMapper.findAppViewInfo(viewIds);
        Map<Long, AppCoreChartViewVO> viewMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(publishedViews)) {
            publishedViews.forEach(view -> viewMap.put(view.getId(), view));
        }
        List<Long> missingViewIds = viewIds.stream()
                .filter(viewId -> !viewMap.containsKey(viewId))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(missingViewIds)) {
            List<AppCoreChartViewVO> snapshotViews = appTemplateMapper.findAppSnapshotViewInfo(missingViewIds);
            if (!CollectionUtils.isEmpty(snapshotViews)) {
                snapshotViews.forEach(view -> viewMap.put(view.getId(), view));
            }
        }
        return viewIds.stream()
                .map(viewMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateAppExportScope(VisualizationAppExportRequest request) {
        if (request == null || request.getDvId() == null) {
            CrestException.throwException("资源不存在");
            return;
        }
        Long dvId = request.getDvId();
        DataVisualizationInfo current = visualizationInfoMapper.selectById(dvId);
        coreVisualizationManage.requireVisualizationManage(current);

        Set<Long> enabledViewIds = new LinkedHashSet<>();
        enabledViewIds.addAll(getEnabledViewIds(dvId, CommonConstants.RESOURCE_TABLE.CORE));
        enabledViewIds.addAll(getEnabledViewIds(dvId, CommonConstants.RESOURCE_TABLE.SNAPSHOT));
        List<Long> requestedViewIds = Optional.ofNullable(request.getViewIds()).orElseGet(List::of);
        if (requestedViewIds.stream().anyMatch(Objects::isNull)
                || !enabledViewIds.containsAll(requestedViewIds)) {
            CrestException.throwException("导出范围包含无效图表");
        }

        List<Long> requestedDsIds = Optional.ofNullable(request.getDsIds()).orElseGet(List::of);
        if (requestedDsIds.stream().anyMatch(Objects::isNull)) {
            CrestException.throwException("导出范围包含无效数据集");
        }
        if (!requestedDsIds.isEmpty()) {
            if (enabledViewIds.isEmpty()) {
                CrestException.throwException("导出范围包含无效数据集");
            }
            Set<Long> allowedDsIds = findAppChartViews(new ArrayList<>(enabledViewIds)).stream()
                    .map(AppCoreChartViewVO::getTableId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!allowedDsIds.containsAll(requestedDsIds)) {
                CrestException.throwException("导出范围包含无效数据集");
            }
        }
    }

    @CrestAudit(id = "#p0.id", ot = LogOT.APP_TEMPLATE_EXPORT, stExp = "#p0.type")
    public void exportLogApp(@RequestBody DataVisualizationBaseRequest request) {

    }

    @CrestAudit(id = "#p0.id", ot = LogOT.TEMPLATE_EXPORT, stExp = "#p0.type")
    public void exportLogTemplate(@RequestBody DataVisualizationBaseRequest request) {

    }

    @CrestAudit(id = "#p0.id", ot = LogOT.PDF_EXPORT, stExp = "#p0.type")
    public void exportLogPDF(@RequestBody DataVisualizationBaseRequest request) {

    }

    @CrestAudit(id = "#p0.id", ot = LogOT.IMG_EXPORT, stExp = "#p0.type")
    public void exportLogImg(@RequestBody DataVisualizationBaseRequest request) {

    }


    @Override
    public void nameCheck(@RequestBody DataVisualizationBaseRequest request) {
        QueryWrapper<DataVisualizationInfo> wrapper = new QueryWrapper<>();
        if (DataVisualizationConstants.RESOURCE_OPT_TYPE.MOVE.equals(request.getOpt()) || DataVisualizationConstants.RESOURCE_OPT_TYPE.RENAME.equals(request.getOpt()) || DataVisualizationConstants.RESOURCE_OPT_TYPE.EDIT.equals(request.getOpt()) || DataVisualizationConstants.RESOURCE_OPT_TYPE.COPY.equals(request.getOpt())) {
            if (request.getPid() == null) {
                DataVisualizationInfo result = visualizationInfoMapper.selectById(request.getId());
                request.setPid(result.getPid());
            }
            if (DataVisualizationConstants.RESOURCE_OPT_TYPE.MOVE.equals(request.getOpt()) || DataVisualizationConstants.RESOURCE_OPT_TYPE.RENAME.equals(request.getOpt()) || DataVisualizationConstants.RESOURCE_OPT_TYPE.EDIT.equals(request.getOpt())) {
                wrapper.ne("id", request.getId());
            }
        }
        wrapper.eq("delete_flag", 0);
        wrapper.eq("pid", request.getPid());
        wrapper.ne("pid", -1);
        wrapper.eq("name", request.getName().trim());
        wrapper.eq("node_type", request.getNodeType());
        wrapper.eq("type", request.getType());
        if (AuthUtils.getUser().getDefaultOid() != null) {
            wrapper.eq("org_id", AuthUtils.getUser().getDefaultOid());
        }
        List<DataVisualizationInfo> existList = visualizationInfoMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(existList) && existList.stream().anyMatch(item -> item.getName().equals(request.getName().trim()))) {
            CrestException.throwException("当前名称已经存在");
        }
    }

    public String getAbsPath(Long id) {
        ChartViewDTO viewDTO = chartViewManege.findChartViewAround(String.valueOf(id));
        if (viewDTO == null) {
            return null;
        }
        if (viewDTO.getPid() == null) {
            return viewDTO.getTitle();
        }
        List<DataVisualizationInfo> parents = getParents(viewDTO.getPid());
        StringBuilder stringBuilder = new StringBuilder();
        parents.forEach(ele -> {
            if (ObjectUtils.isNotEmpty(ele)) {
                stringBuilder.append(ele.getName()).append("/");
            }
        });
        stringBuilder.append(viewDTO.getTitle());
        return stringBuilder.toString();
    }

    public List<DataVisualizationInfo> getParents(Long id) {
        List<DataVisualizationInfo> list = new ArrayList<>();
        DataVisualizationInfo dataVisualizationInfo = visualizationInfoMapper.selectById(id);
        list.add(dataVisualizationInfo);
        if (dataVisualizationInfo.getPid().equals(dataVisualizationInfo.getId())) {
            return list;
        }
        getParent(list, dataVisualizationInfo);
        Collections.reverse(list);
        return list;
    }

    public void getParent(List<DataVisualizationInfo> list, DataVisualizationInfo dataVisualizationInfo) {
        if (ObjectUtils.isNotEmpty(dataVisualizationInfo) && dataVisualizationInfo.getPid() != null && !dataVisualizationInfo.getPid().equals(dataVisualizationInfo.getId())) {
            DataVisualizationInfo d = visualizationInfoMapper.selectById(dataVisualizationInfo.getPid());
            list.add(d);
            getParent(list, d);
        }
    }

    public List<Long> getEnabledViewIds(Long dvId, String resourceTable) {
        List<Long> result = new ArrayList<>();
        DataVisualizationVO dvInfo = extDataVisualizationMapper.findDvInfo(dvId, null, resourceTable);
        List<CoreChartView> views = extChartViewMapper.selectListCustom(dvId, resourceTable);
        if (CollectionUtils.isNotEmpty(views) && dvInfo != null) {
            String componentData = dvInfo.getComponentData();
            result = views.stream().filter(item -> componentData.indexOf("\"id\":\"" + item.getId()) > 0).map(CoreChartView::getId).collect(Collectors.toList());

        }
        return result;
    }

    private void requireVisualizationRead(Long dvId) {
        if (dvId == null) {
            CrestException.throwException("资源不存在");
            return;
        }
        DataVisualizationInfo info = visualizationInfoMapper.selectById(dvId);
        if (info == null) {
            CrestException.throwException("资源不存在");
            return;
        }
        if (CrestPermissionUtils.currentUserId() != null
                && !dataPortalPermissionManage.canReadPublishedVisualization(dvId, info.getType())) {
            CrestPermissionUtils.requireCreator(info.getCreateBy());
        }
    }

    @Resource
    private ResourcePermissionManage resourcePermissionManage;
    @Override
    public List<DatasetPermissionTemplate> queruDatasetPermissionTemplate(Long resourceId) {
        return resourcePermissionManage.queruDatasetPermissionTemplate(resourceId);
    }
}
