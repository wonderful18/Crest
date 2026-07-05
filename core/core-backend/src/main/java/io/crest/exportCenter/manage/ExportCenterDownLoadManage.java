package io.crest.exportCenter.manage;


import com.fasterxml.jackson.core.type.TypeReference;
import io.crest.api.chart.dto.ChartSortFieldDTO;
import io.crest.api.chart.dto.ViewDetailField;
import io.crest.api.chart.request.ChartExcelRequest;
import io.crest.api.chart.request.ChartExcelRequestInner;
import io.crest.api.dataset.dto.DataSetExportRequest;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.api.permissions.dataset.dto.DataSetRowPermissionsTreeDTO;
import io.crest.api.permissions.user.vo.UserFormVO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.chart.dao.auto.mapper.CoreChartViewMapper;
import io.crest.chart.server.ChartDataServer;
import io.crest.commons.utils.ExcelWatermarkUtils;
import io.crest.constant.FieldTypeConstants;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.dataset.dao.auto.entity.CoreDatasetGroup;
import io.crest.dataset.dao.auto.mapper.CoreDatasetGroupMapper;
import io.crest.dataset.manage.*;
import io.crest.datasource.utils.DatasourceUtils;
import io.crest.engine.sql.SQLProvider;
import io.crest.engine.trans.*;
import io.crest.engine.utils.Utils;
import io.crest.exception.CrestException;
import io.crest.exportCenter.dao.auto.entity.CoreExportTask;
import io.crest.exportCenter.dao.auto.mapper.CoreExportTaskMapper;
import io.crest.exportCenter.util.ExportCenterUtils;
import io.crest.extensions.datasource.api.PluginManageApi;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.factory.ProviderFactory;
import io.crest.extensions.datasource.model.SQLMeta;
import io.crest.extensions.datasource.provider.Provider;
import io.crest.extensions.view.dto.ChartViewDTO;
import io.crest.extensions.view.dto.ChartViewFieldDTO;
import io.crest.extensions.view.dto.ColumnPermissionItem;
import io.crest.extensions.view.dto.DatasetRowPermissionsTreeObj;
import io.crest.i18n.Translator;
import io.crest.log.CrestAudit;
import io.crest.model.ExportTaskDTO;
import io.crest.utils.*;
import io.crest.visualization.dao.auto.entity.VisualizationWatermark;
import io.crest.visualization.dao.auto.mapper.VisualizationWatermarkMapper;
import io.crest.visualization.dao.ext.mapper.ExtDataVisualizationMapper;
import io.crest.visualization.dto.WatermarkContentDTO;
import io.crest.websocket.WsMessage;
import io.crest.websocket.WsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = Exception.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class ExportCenterDownLoadManage {
    private static final int EXCEL_NUMERIC_TEXT_LIMIT = 64;
    private static final long KIB = 1024L;
    private static final long MIB = KIB * 1024L;
    private static final long GIB = MIB * 1024L;
    private static final Pattern EXCEL_NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    @Resource
    private CoreExportTaskMapper exportTaskMapper;
    @Resource
    private CoreChartViewMapper coreChartViewMapper;
    @Resource
    private PermissionManage permissionManage;
    @Resource
    private DatasetGroupManage datasetGroupManage;
    @Autowired
    private WsService wsService;
    @Autowired(required = false)
    private PluginManageApi pluginManage;
    @Value("${crest.export.core.size:10}")
    private int core;
    @Value("${crest.export.max.size:10}")
    private int max;

    @Value("${crest.path.exportData:/opt/crest/data/exportData/}")
    private String exportData_path;
    @Resource
    private VisualizationWatermarkMapper watermarkMapper;
    @Resource
    private ExtDataVisualizationMapper visualizationMapper;
    @Value("${crest.export.page.size:50000}")
    private Integer extractPageSize;
    static private List<String> STATUS = Arrays.asList("SUCCESS", "FAILED", "PENDING", "IN_PROGRESS", "ALL");
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private int keepAliveSeconds = 600;
    private Map<String, Future> Running_Task = new HashMap<>();
    @Resource
    private ChartDataServer chartDataServer;
    @Resource
    private CoreDatasetGroupMapper coreDatasetGroupMapper;
    @Resource
    private DatasetSQLManage datasetSQLManage;
    @Resource
    private DatasetTableFieldManage datasetTableFieldManage;
    @Resource
    private DatasetDataManage datasetDataManage;
    private final Long sheetLimit = 1000000L;
    @PostConstruct
    public void init() {
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(core);
        scheduledThreadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.setMaximumPoolSize(max);
    }

    @Scheduled(fixedRate = 5000)
    public void checkRunningTask() {
        Iterator<Map.Entry<String, Future>> iterator = Running_Task.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Future> entry = iterator.next();
            if (entry.getValue().isDone()) {
                iterator.remove();
                try {
                    CoreExportTask exportTask = exportTaskMapper.selectById(entry.getKey());
                    ExportTaskDTO exportTaskDTO = new ExportTaskDTO();
                    BeanUtils.copyBean(exportTaskDTO, exportTask);
                    setExportFromName(exportTaskDTO);
                    WsMessage message = new WsMessage(exportTask.getUserId(), "/task-export-topic", exportTaskDTO);
                    wsService.releaseMessage(message);
                } catch (RuntimeException e) {
                    LogUtil.debug("Release export task message failed: " + entry.getKey());
                }
            }
        }
    }

    private void setExportFromName(ExportTaskDTO exportTaskDTO) {
        if (exportTaskDTO.getExportFromType().equalsIgnoreCase("chart")) {
            exportTaskDTO.setExportFromName(coreChartViewMapper.selectById(exportTaskDTO.getExportFrom()).getTitle());
        }
        if (exportTaskDTO.getExportFromType().equalsIgnoreCase("dataset")) {
            exportTaskDTO.setExportFromName(coreDatasetGroupMapper.selectById(exportTaskDTO.getExportFrom()).getName());
        }
    }

    @CrestAudit(id = "#p0.exportFrom", ot = LogOT.EXPORT, st = LogST.DATASET)
    public void startDatasetTask(CoreExportTask exportTask, DataSetExportRequest request) {
        File directory = prepareTaskDirectory(exportTask.getId());
        File exportFile = resolveExportFile(directory, exportTask.getId());

        TokenUserBO tokenUserBO = AuthUtils.getUser();
        Future future = scheduledThreadPoolExecutor.submit(() -> {
            AuthUtils.setUser(tokenUserBO);
            try {
                exportTask.setExportStatus("IN_PROGRESS");
                exportTask.setMsg(null);
                exportTask.setFileSize(null);
                exportTask.setFileSizeUnit(null);
                exportTaskMapper.updateById(exportTask);
                CoreDatasetGroup coreDatasetGroup = coreDatasetGroupMapper.selectById(exportTask.getExportFrom());
                if (coreDatasetGroup == null) {
                    CrestException.throwException("Not found dataset group: " + exportTask.getExportFrom());
                }
                DatasetGroupInfoDTO dto = new DatasetGroupInfoDTO();
                BeanUtils.copyBean(dto, coreDatasetGroup);
                dto.setUnionSql(null);
                List<UnionDTO> unionDTOList = JsonUtil.parseList(coreDatasetGroup.getInfo(), new TypeReference<>() {
                });
                dto.setUnion(unionDTOList);
                List<DatasetTableFieldDTO> dsFields = datasetTableFieldManage.selectByDatasetGroupId(Long.valueOf(exportTask.getExportFrom()));
                List<DatasetTableFieldDTO> allFields = dsFields.stream().map(ele -> {
                    DatasetTableFieldDTO datasetTableFieldDTO = new DatasetTableFieldDTO();
                    BeanUtils.copyBean(datasetTableFieldDTO, ele);
                    datasetTableFieldDTO.setFieldShortName(ele.getEngineFieldName());
                    return datasetTableFieldDTO;
                }).collect(Collectors.toList());
                DatasetGroupInfoDTO datasetGroupInfoDTO = datasetGroupManage.datasetGroupInfoDTO(request.getId(), null);
                Map<String, Object> sqlMap = datasetSQLManage.getUnionSQLForEdit(datasetGroupInfoDTO, null);
                if (sqlMap == null || StringUtils.isBlank((String) sqlMap.get("sql"))) {
                    CrestException.throwException("数据集配置不完整，无法导出");
                }
                String sql = (String) sqlMap.get("sql");
                if (ObjectUtils.isEmpty(allFields)) {
                    CrestException.throwException(Translator.get("i18n_no_fields"));
                }
                Map<String, ColumnPermissionItem> desensitizationList = new HashMap<>();
                allFields = permissionManage.filterColumnPermissions(allFields, desensitizationList, dto.getId(), null);
                if (ObjectUtils.isEmpty(allFields)) {
                    CrestException.throwException(Translator.get("i18n_no_column_permission"));
                }
                dto.setAllFields(allFields);
                datasetDataManage.buildFieldName(sqlMap, allFields);
                Map<Long, DatasourceSchemaDTO> dsMap = (Map<Long, DatasourceSchemaDTO>) sqlMap.get("dsMap");
                DatasourceUtils.checkDsStatus(dsMap);
                List<String> dsList = new ArrayList<>();
                for (Map.Entry<Long, DatasourceSchemaDTO> next : dsMap.entrySet()) {
                    dsList.add(next.getValue().getType());
                }
                boolean needOrder = Utils.isNeedOrder(dsList);
                boolean crossDs = dto.getIsCross();
                if (!crossDs) {
                    if (datasetDataManage.notFullDs.contains(dsMap.entrySet().iterator().next().getValue().getType()) && (boolean) sqlMap.get("isFullJoin")) {
                        CrestException.throwException(Translator.get("i18n_not_full"));
                    }
                    sql = Utils.replaceSchemaAlias(sql, dsMap);
                }
                List<DataSetRowPermissionsTreeDTO> rowPermissionsTree = new ArrayList<>();
                TokenUserBO user = AuthUtils.getUser();
                if (user != null) {
                    rowPermissionsTree = permissionManage.getRowPermissionsTree(dto.getId(), user.getUserId());
                }
                if (StringUtils.isNotEmpty(request.getExpressionTree())) {
                    DatasetRowPermissionsTreeObj datasetRowPermissionsTreeObj = JsonUtil.parseObject(request.getExpressionTree(), DatasetRowPermissionsTreeObj.class);
                    permissionManage.getField(datasetRowPermissionsTreeObj);
                    DataSetRowPermissionsTreeDTO dataSetRowPermissionsTreeDTO = new DataSetRowPermissionsTreeDTO();
                    dataSetRowPermissionsTreeDTO.setTree(datasetRowPermissionsTreeObj);
                    dataSetRowPermissionsTreeDTO.setExportData(true);
                    rowPermissionsTree.add(dataSetRowPermissionsTreeDTO);
                }

                Provider provider;
                if (crossDs) {
                    provider = ProviderFactory.getDefaultProvider();
                } else {
                    provider = ProviderFactory.getProvider(dsList.getFirst());
                }
                SQLMeta sqlMeta = new SQLMeta();
                Table2SQLObj.table2sqlobj(sqlMeta, null, "(" + sql + ")", crossDs);
                Field2SQLObj.field2sqlObj(sqlMeta, allFields, allFields, crossDs, dsMap, Utils.getParams(allFields), null, pluginManage);
                WhereTree2Str.transFilterTrees(sqlMeta, rowPermissionsTree, allFields, crossDs, dsMap, Utils.getParams(allFields), null, pluginManage);
                List<ChartSortFieldDTO> sortFields = new ArrayList<>();
                for (DatasetTableFieldDTO field : allFields) {
                    if (field.getOrderChecked()) {
                        ChartSortFieldDTO sortField = new ChartSortFieldDTO();
                        BeanUtils.copyBean(sortField, field);
                        sortField.setOrderDirection("asc");
                        sortFields.add(sortField);
                    }
                }
                dto.setSortFields(sortFields);
                DatasetOrder2SQLObj.getOrders(sqlMeta, dto.getSortFields(), allFields);
                String replaceSql = provider.rebuildSQL(SQLProvider.createQuerySQL(sqlMeta, false, false, false), sqlMeta, crossDs, dsMap);
                Long totalCount = datasetDataManage.datasetTotal(dto, replaceSql, null);
                totalCount = Objects.requireNonNullElse(totalCount, 0L);
                Long curLimit = ExportCenterUtils.getExportLimit("dataset");
                totalCount = totalCount > curLimit ? curLimit : totalCount;

                Long sheetCount = (totalCount / sheetLimit) + (totalCount % sheetLimit > 0 ? 1 : 0);
                try (Workbook wb = new SXSSFWorkbook()) {
                    CellStyle cellStyle = wb.createCellStyle();
                    Font font = wb.createFont();
                    font.setFontHeightInPoints((short) 12);
                    font.setBold(true);
                    cellStyle.setFont(font);
                    cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    for (Long s = 1L; s < sheetCount + 1; s++) {
                        Long sheetSize;
                        if (s.equals(sheetCount)) {
                            sheetSize = totalCount - (s - 1) * sheetLimit;
                        } else {
                            sheetSize = sheetLimit;
                        }
                        Long pageSize = (sheetSize / extractPageSize) + (sheetSize % extractPageSize > 0 ? 1 : 0);
                        Sheet detailsSheet = null;
                        List<List<String>> details = new ArrayList<>();
                        for (Long p = 0L; p < pageSize; p++) {
                            int beforeCount = (int) ((s - 1) * sheetLimit);
                            String querySQL = SQLProvider.createQuerySQLWithLimit(sqlMeta, false, needOrder, false, beforeCount + p.intValue() * extractPageSize, extractPageSize);
                            if (pageSize == 1) {
                                querySQL = SQLProvider.createQuerySQLWithLimit(sqlMeta, false, needOrder, false, 0, sheetSize.intValue());
                            }
                            querySQL = provider.rebuildSQL(querySQL, sqlMeta, crossDs, dsMap);
                            DatasourceRequest datasourceRequest = new DatasourceRequest();
                            datasourceRequest.setQuery(querySQL);
                            datasourceRequest.setDsList(dsMap);
                            datasourceRequest.setIsCross(coreDatasetGroup.getIsCross());
                            Map<String, Object> previewData = datasetDataManage.buildPreviewData(provider.fetchResultField(datasourceRequest), allFields, desensitizationList, false);
                            List<Map<String, Object>> data = (List<Map<String, Object>>) previewData.get("data");
                            if (p.equals(0L)) {
                                detailsSheet = wb.createSheet("数据" + s);
                                List<String> header = new ArrayList<>();
                                for (DatasetTableFieldDTO field : allFields) {
                                    header.add(field.getName());
                                }
                                details.add(header);
                                for (Map<String, Object> obj : data) {
                                    List<String> row = new ArrayList<>();
                                    for (DatasetTableFieldDTO field : allFields) {
                                        String string = (String) obj.get(field.getEngineFieldName());
                                        row.add(string);
                                    }
                                    details.add(row);
                                }
                                if (CollectionUtils.isNotEmpty(details)) {
                                    for (int i = 0; i < details.size(); i++) {
                                        Row row = detailsSheet.createRow(i);
                                        List<String> rowData = details.get(i);
                                        if (rowData != null) {
                                            for (int j = 0; j < rowData.size(); j++) {
                                                Cell cell = row.createCell(j);
                                                if (i == 0) {
                                                    cell.setCellValue(rowData.get(j));
                                                    cell.setCellStyle(cellStyle);
                                                    detailsSheet.setColumnWidth(j, 255 * 20);
                                                } else {
                                                    setExportCellValue(cell, allFields.get(j).getFieldType(), rowData.get(j));
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                details.clear();
                                for (Map<String, Object> obj : data) {
                                    List<String> row = new ArrayList<>();
                                    for (DatasetTableFieldDTO field : allFields) {
                                        String string = (String) obj.get(field.getEngineFieldName());
                                        row.add(string);
                                    }
                                    details.add(row);
                                }
                                int lastNum = detailsSheet.getLastRowNum();
                                for (int i = 0; i < details.size(); i++) {
                                    Row row = detailsSheet.createRow(i + lastNum + 1);
                                    List<String> rowData = details.get(i);
                                    if (rowData != null) {
                                        for (int j = 0; j < rowData.size(); j++) {
                                            Cell cell = row.createCell(j);
                                            setExportCellValue(cell, allFields.get(j).getFieldType(), rowData.get(j));
                                        }
                                    }
                                }
                            }
                            exportTask.setExportStatus("IN_PROGRESS");
                            exportTask.setExportProgress(formatPagedProgress(s - 1, p + 1, pageSize, sheetCount));
                            exportTaskMapper.updateById(exportTask);
                        }
                    }
                    this.addWatermarkTools(wb);
                    try (FileOutputStream fileOutputStream = new FileOutputStream(exportFile)) {
                        wb.write(fileOutputStream);
                        fileOutputStream.flush();
                    }
                }
                exportTask.setExportProgress("100");
                exportTask.setExportStatus("SUCCESS");
                exportTask.setMsg(null);
                setFileSize(exportFile, exportTask);

            } catch (IOException | RuntimeException e) {
                LogUtil.error("Failed to export data", e);
                exportTask.setMsg(e.getMessage());
                exportTask.setExportStatus("FAILED");
            } finally {
                exportTaskMapper.updateById(exportTask);
            }
        });
        Running_Task.put(exportTask.getId(), future);
    }

    @CrestAudit(id = "#p0.exportFrom", ot = LogOT.EXPORT, st = LogST.PANEL)
    public void startPanelViewTask(CoreExportTask exportTask, ChartExcelRequest request) {
        startViewTask(exportTask, request);
    }

    @CrestAudit(id = "#p0.exportFrom", ot = LogOT.EXPORT, st = LogST.SCREEN)
    public void startDataVViewTask(CoreExportTask exportTask, ChartExcelRequest request) {
        startViewTask(exportTask, request);
    }

    public static void removeColumn(List<Object[]> list, List<Integer> columnIndexs) {
        if (list == null || list.isEmpty() || columnIndexs == null || columnIndexs.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Object[] originalRow = list.get(i);
            Object[] newRow = new Object[originalRow.length - columnIndexs.size()];
            int newIndex = 0;
            for (int j = 0; j < originalRow.length; j++) {
                if (!columnIndexs.contains(j)) {
                    newRow[newIndex++] = originalRow[j];
                }
            }
            list.set(i, newRow);
        }
    }

    private boolean isNumericExportField(Integer fieldType) {
        return Objects.equals(fieldType, FieldTypeConstants.INTEGER) || Objects.equals(fieldType, FieldTypeConstants.FLOAT);
    }

    private void setExportCellValue(Cell cell, Integer fieldType, String value) {
        if (isNumericExportField(fieldType) && isSafeExcelNumericText(value)) {
            try {
                cell.setCellValue(Double.parseDouble(StringUtils.trim(value)));
                return;
            } catch (NumberFormatException ignored) {
                // 保持原有导出行为：无法安全转成数字时按文本写入。
            }
        }
        cell.setCellValue(value);
    }

    private boolean isSafeExcelNumericText(String value) {
        String text = StringUtils.trimToEmpty(value);
        return StringUtils.isNotEmpty(text)
                && text.length() <= EXCEL_NUMERIC_TEXT_LIMIT
                && EXCEL_NUMERIC_PATTERN.matcher(text).matches();
    }

    public void startViewTask(CoreExportTask exportTask, ChartExcelRequest request) {
        File directory = prepareTaskDirectory(exportTask.getId());
        File exportFile = resolveExportFile(directory, exportTask.getId());
        TokenUserBO tokenUserBO = AuthUtils.getUser();
        Future future = scheduledThreadPoolExecutor.submit(() -> {
            AuthUtils.setUser(tokenUserBO);
            try {
                exportTask.setExportStatus("IN_PROGRESS");
                exportTask.setMsg(null);
                exportTask.setFileSize(null);
                exportTask.setFileSizeUnit(null);
                exportTaskMapper.updateById(exportTask);
                try (Workbook wb = new SXSSFWorkbook()) {
                    CellStyle cellStyle = wb.createCellStyle();
                    Font font = wb.createFont();
                    font.setFontHeightInPoints((short) 12);
                    font.setBold(true);
                    cellStyle.setFont(font);
                    cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    List<Object[]> details = new ArrayList<>();
                    Sheet detailsSheet;
                    Integer sheetIndex = 1;
                    if ("dataset".equals(request.getDownloadType()) || request.getViewInfo().getType().equalsIgnoreCase("table-info") || request.getViewInfo().getType().equalsIgnoreCase("table-normal")) {
                        boolean summaryEnabled = !"dataset".equals(request.getDownloadType()) && ChartDataServer.isSummaryEnabled(request.getViewInfo());
                        ChartDataServer.SummaryConfig summaryConfig = null;
                        ChartDataServer.SummaryAccumulator summaryAcc = new ChartDataServer.SummaryAccumulator();
                        List<ChartViewFieldDTO> allExportColumns = Collections.emptyList();
                        Map<String, java.math.BigDecimal> customSumResult = null;
                        if (summaryEnabled) {
                            summaryConfig = ChartDataServer.parseSummaryConfig(request.getViewInfo());
                            allExportColumns = ChartDataServer.getAllExportColumns(request.getViewInfo());
                        }

                        request.getViewInfo().getChartExtRequest().setPageSize(Long.valueOf(extractPageSize));
                        ChartViewDTO chartViewDTO = chartDataServer.findExcelData(request);
                        for (long i = 1; i < chartViewDTO.getTotalPage() + 1; i++) {
                            request.getViewInfo().getChartExtRequest().setGoPage(i);
                            request.getViewInfo().setExtStack(request.getViewInfo().getExtStack().stream().filter(ele -> !ele.isHide()).collect(Collectors.toList()));
                            ChartViewDTO pageDto = chartDataServer.findExcelData(request);
                            details.addAll(request.getDetails());

                            if (summaryEnabled) {
                                ChartDataServer.accumulatePageStats(summaryAcc, request.getDetails(), allExportColumns, summaryConfig);
                                if (i == chartViewDTO.getTotalPage() && pageDto.getData() != null && pageDto.getData().get("customSumResult") != null) {
                                    customSumResult = (Map<String, java.math.BigDecimal>) pageDto.getData().get("customSumResult");
                                }
                            }

                            if (((details.size() + extractPageSize) > sheetLimit) || i == chartViewDTO.getTotalPage()) {
                                if (i == chartViewDTO.getTotalPage() && summaryEnabled && summaryAcc.totalCount > 0) {
                                    Object[] totalRow = ChartDataServer.buildSummaryRow(allExportColumns, summaryConfig, summaryAcc, customSumResult);
                                    details.add(totalRow);
                                }

                                detailsSheet = wb.createSheet("数据" + sheetIndex);
                                Integer[] excelTypes = request.getExcelTypes();
                                ViewDetailField[] detailFields = request.getDetailFields();
                                Object[] header = request.getHeader();
                                List<ChartViewFieldDTO> xAxis = new ArrayList<>();
                                xAxis.addAll(request.getViewInfo().getXAxis());
                                xAxis.addAll(request.getViewInfo().getYAxis());
                                xAxis.addAll(request.getViewInfo().getXAxisExt());
                                xAxis.addAll(request.getViewInfo().getYAxisExt());
                                xAxis.addAll(request.getViewInfo().getExtStack());
                                xAxis.addAll(request.getViewInfo().getDrillFields());
                                header = Arrays.stream(request.getHeader()).filter(item -> xAxis.stream().map(d -> StringUtils.isNotBlank(d.getChartShowName()) ? d.getChartShowName() : d.getName()).toList().contains(item)).toArray();
                                details.add(0, header);
                                List<Integer> columnIndexs = new ArrayList<>();
                                for (int i1 = 0; i1 < xAxis.size(); i1++) {
                                    ChartViewFieldDTO xAxi = xAxis.get(i1);
                                    if (xAxi.isHide()) {
                                        columnIndexs.add(i1);
                                    }
                                }
                                removeColumn(details, columnIndexs);
                                ChartDataServer.setExcelData(detailsSheet, cellStyle, header, details, detailFields, excelTypes, request.getViewInfo(), wb);
                                sheetIndex++;
                                details.clear();
                            }
                            exportTask.setExportStatus("IN_PROGRESS");
                            exportTask.setExportProgress(formatProgress(i, chartViewDTO.getTotalPage()));
                            exportTaskMapper.updateById(exportTask);
                        }
                    } else {
                        downloadNotTableInfoData(request, wb);
                    }
                    this.addWatermarkTools(wb);

                    try (FileOutputStream outputStream = new FileOutputStream(exportFile)) {
                        wb.write(outputStream);
                        outputStream.flush();
                    }
                }
                exportTask.setExportProgress("100");
                exportTask.setExportStatus("SUCCESS");
                exportTask.setMsg(null);
                setFileSize(exportFile, exportTask);
            } catch (IOException | RuntimeException e) {
                exportTask.setMsg(e.getMessage());
                LogUtil.error("Failed to export data", e);
                exportTask.setExportStatus("FAILED");
            } finally {
                exportTaskMapper.updateById(exportTask);
            }
        });
        Running_Task.put(exportTask.getId(), future);
    }

    private void downloadNotTableInfoData(ChartExcelRequest request, Workbook wb) {
        chartDataServer.findExcelData(request);
        //给单元格设置样式
        CellStyle cellStyle = wb.createCellStyle();
        Font font = wb.createFont();
        //设置字体大小
        font.setFontHeightInPoints((short) 12);
        //设置字体加粗
        font.setBold(true);
        //给字体设置样式
        cellStyle.setFont(font);
        //设置单元格背景颜色
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        //设置单元格填充样式(使用纯色背景颜色填充)
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (CollectionUtils.isEmpty(request.getMultiInfo())) {
            if (!request.getViewInfo().getType().equalsIgnoreCase("chart-mix-dual-line")) {
                List<Object[]> details = request.getDetails();
                Integer[] excelTypes = request.getExcelTypes();
                details.add(0, request.getHeader());
                ViewDetailField[] detailFields = request.getDetailFields();
                Object[] header = request.getHeader();
                Sheet detailsSheet = wb.createSheet("数据");
                if (request.getViewInfo().getType().equalsIgnoreCase("table-normal")) {
                    ChartDataServer.setExcelData(detailsSheet, cellStyle, header, details, detailFields, excelTypes, request.getViewInfo(), wb);
                } else {
                    ChartDataServer.setExcelData(detailsSheet, cellStyle, header, details, detailFields, excelTypes, request.getViewInfo(), null);
                }
            }
        } else {
            //多个sheet
            for (int i = 0; i < request.getMultiInfo().size(); i++) {
                ChartExcelRequestInner requestInner = request.getMultiInfo().get(i);

                List<Object[]> details = requestInner.getDetails();
                Integer[] excelTypes = requestInner.getExcelTypes();
                details.add(0, requestInner.getHeader());
                ViewDetailField[] detailFields = requestInner.getDetailFields();
                Object[] header = requestInner.getHeader();
                //明细sheet
                Sheet detailsSheet = wb.createSheet("数据 " + (i + 1));
                ChartDataServer.setExcelData(detailsSheet, cellStyle, header, details, detailFields, excelTypes, request.getViewInfo(), null);
            }
        }
    }

    private void setFileSize(File file, CoreExportTask exportTask) {
        long length = file.length();
        String unit;
        double size;
        if (length > MIB) {
            if (length > GIB) {
                unit = "Gb";
                size = displaySize(length, GIB);
            } else {
                unit = "Mb";
                size = displaySize(length, MIB);
            }
        } else {
            unit = "Kb";
            size = displaySize(length, KIB);
        }
        exportTask.setFileSize(size);
        exportTask.setFileSizeUnit(unit);
    }

    private String formatPagedProgress(long completedSheets, long currentPage, long pageSize, long sheetCount) {
        if (pageSize <= 0 || sheetCount <= 0) {
            return "0";
        }
        BigDecimal completed = BigDecimal.valueOf(completedSheets);
        BigDecimal current = BigDecimal.valueOf(currentPage)
                .divide(BigDecimal.valueOf(pageSize), 6, RoundingMode.HALF_UP);
        return completed.add(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sheetCount), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String formatProgress(long current, long total) {
        if (total <= 0) {
            return "0";
        }
        return BigDecimal.valueOf(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private double displaySize(long length, long unitBytes) {
        return BigDecimal.valueOf(length)
                .divide(BigDecimal.valueOf(unitBytes), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private File prepareTaskDirectory(String taskId) {
        try {
            return FileUtils.ensureSubDirectory(exportData_path, taskId);
        } catch (IOException e) {
            CrestException.throwException("导出目录创建失败: " + e.getMessage());
            return null;
        }
    }

    private File resolveTaskDirectory(String taskId) {
        try {
            return FileUtils.resolveSubDirectory(exportData_path, taskId);
        } catch (IOException e) {
            CrestException.throwException("导出目录无效: " + e.getMessage());
            return null;
        }
    }

    private File resolveExportFile(File directory, String taskId) {
        try {
            return FileUtils.resolveUnderDirectory(directory.getPath(), taskId + ".xlsx");
        } catch (IOException e) {
            CrestException.throwException("导出文件路径无效: " + e.getMessage());
            return null;
        }
    }

    public void addWatermarkTools(Workbook wb) {
        VisualizationWatermark watermark = watermarkMapper.selectById("system_default");
        WatermarkContentDTO watermarkContent = JsonUtil.parseObject(watermark.getSettingContent(), WatermarkContentDTO.class);
        if (watermarkContent.getEnable() && watermarkContent.getExcelEnable()) {
            UserFormVO userInfo = visualizationMapper.queryInnerUserInfo(AuthUtils.getUser().getUserId());
            // 在主逻辑中添加水印
            int watermarkPictureIdx = ExcelWatermarkUtils.addWatermarkImage(wb, watermarkContent, userInfo); // 生成水印图片并获取 ID
            for (Sheet sheet : wb) {
                ExcelWatermarkUtils.addWatermarkToSheet(sheet, watermarkPictureIdx); // 为每个 Sheet 添加水印
            }
        }
    }

    public void download(CoreExportTask exportTask, HttpServletResponse response) throws IOException {
        File exportFile;
        File exportDirectory = resolveTaskDirectory(exportTask.getId());
        if (exportTask.getExportTime() != null && exportTask.getExportTime() < 1730277243491L) {
            exportFile = FileUtils.resolveUnderDirectory(exportDirectory.getPath(), exportTask.getFileName());
        } else {
            exportFile = FileUtils.resolveUnderDirectory(exportDirectory.getPath(), exportTask.getId() + ".xlsx");
        }
        if (!exportFile.isFile()) {
            CrestException.throwException(Translator.get("i18n_export_file_missing"));
        }

        response.setContentType("application/octet-stream");
        String encodedFileName = URLEncoder.encode(exportTask.getFileName(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);

        try (FileInputStream fileInputStream = new FileInputStream(exportFile); OutputStream outputStream = response.getOutputStream()) {
            FileChannel fileChannel = fileInputStream.getChannel();
            WritableByteChannel outputChannel = Channels.newChannel(outputStream);
            fileChannel.transferTo(0, fileChannel.size(), outputChannel);
            response.flushBuffer();
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @CrestAudit(id = "#p0.id", ot = LogOT.EXPORT, st = LogST.DATASET)
    public void downloadDataset(DataSetExportRequest request, HttpServletResponse response) throws IOException {
        try (OutputStream outputStream = response.getOutputStream();
             Workbook wb = new SXSSFWorkbook()) {
            CoreDatasetGroup coreDatasetGroup = coreDatasetGroupMapper.selectById(request.getId());
            if (coreDatasetGroup == null) {
                CrestException.throwException("Not found dataset group: " + request.getFilename());
            }
            DatasetGroupInfoDTO dto = new DatasetGroupInfoDTO();
            BeanUtils.copyBean(dto, coreDatasetGroup);
            dto.setUnionSql(null);
            List<UnionDTO> unionDTOList = JsonUtil.parseList(coreDatasetGroup.getInfo(), new TypeReference<>() {
            });
            dto.setUnion(unionDTOList);
            List<DatasetTableFieldDTO> dsFields = datasetTableFieldManage.selectByDatasetGroupId(request.getId());
            List<DatasetTableFieldDTO> allFields = dsFields.stream().map(ele -> {
                DatasetTableFieldDTO datasetTableFieldDTO = new DatasetTableFieldDTO();
                BeanUtils.copyBean(datasetTableFieldDTO, ele);
                datasetTableFieldDTO.setFieldShortName(ele.getEngineFieldName());
                return datasetTableFieldDTO;
            }).collect(Collectors.toList());
            DatasetGroupInfoDTO datasetGroupInfoDTO = datasetGroupManage.datasetGroupInfoDTO(request.getId(), null);
            Map<String, Object> sqlMap = datasetSQLManage.getUnionSQLForEdit(datasetGroupInfoDTO, null);
            if (sqlMap == null || StringUtils.isBlank((String) sqlMap.get("sql"))) {
                CrestException.throwException("数据集配置不完整，无法导出");
            }
            String sql = (String) sqlMap.get("sql");
            if (ObjectUtils.isEmpty(allFields)) {
                CrestException.throwException(Translator.get("i18n_no_fields"));
            }
            Map<String, ColumnPermissionItem> desensitizationList = new HashMap<>();
            allFields = permissionManage.filterColumnPermissions(allFields, desensitizationList, dto.getId(), null);
            if (ObjectUtils.isEmpty(allFields)) {
                CrestException.throwException(Translator.get("i18n_no_column_permission"));
            }
            dto.setAllFields(allFields);
            datasetDataManage.buildFieldName(sqlMap, allFields);
            Map<Long, DatasourceSchemaDTO> dsMap = (Map<Long, DatasourceSchemaDTO>) sqlMap.get("dsMap");
            DatasourceUtils.checkDsStatus(dsMap);
            List<String> dsList = new ArrayList<>();
            for (Map.Entry<Long, DatasourceSchemaDTO> next : dsMap.entrySet()) {
                dsList.add(next.getValue().getType());
            }
            boolean needOrder = Utils.isNeedOrder(dsList);
            boolean crossDs = dto.getIsCross();
            if (!crossDs) {
                if (datasetDataManage.notFullDs.contains(dsMap.entrySet().iterator().next().getValue().getType()) && (boolean) sqlMap.get("isFullJoin")) {
                    CrestException.throwException(Translator.get("i18n_not_full"));
                }
                sql = Utils.replaceSchemaAlias(sql, dsMap);
            }
            List<DataSetRowPermissionsTreeDTO> rowPermissionsTree = new ArrayList<>();
            TokenUserBO user = AuthUtils.getUser();
            if (user != null) {
                rowPermissionsTree = permissionManage.getRowPermissionsTree(dto.getId(), user.getUserId());
            }
            if (StringUtils.isNotEmpty(request.getExpressionTree())) {
                DatasetRowPermissionsTreeObj datasetRowPermissionsTreeObj = JsonUtil.parseObject(request.getExpressionTree(), DatasetRowPermissionsTreeObj.class);
                permissionManage.getField(datasetRowPermissionsTreeObj);
                DataSetRowPermissionsTreeDTO dataSetRowPermissionsTreeDTO = new DataSetRowPermissionsTreeDTO();
                dataSetRowPermissionsTreeDTO.setTree(datasetRowPermissionsTreeObj);
                dataSetRowPermissionsTreeDTO.setExportData(true);
                rowPermissionsTree.add(dataSetRowPermissionsTreeDTO);
            }

            Provider provider;
            if (crossDs) {
                provider = ProviderFactory.getDefaultProvider();
            } else {
                provider = ProviderFactory.getProvider(dsList.getFirst());
            }
            SQLMeta sqlMeta = new SQLMeta();
            Table2SQLObj.table2sqlobj(sqlMeta, null, "(" + sql + ")", crossDs);
            Field2SQLObj.field2sqlObj(sqlMeta, allFields, allFields, crossDs, dsMap, Utils.getParams(allFields), null, pluginManage);
            WhereTree2Str.transFilterTrees(sqlMeta, rowPermissionsTree, allFields, crossDs, dsMap, Utils.getParams(allFields), null, pluginManage);
            Order2SQLObj.getOrders(sqlMeta, dto.getSortFields(), allFields, crossDs, dsMap, Utils.getParams(allFields), null, pluginManage);
            String replaceSql = provider.rebuildSQL(SQLProvider.createQuerySQL(sqlMeta, false, false, false), sqlMeta, crossDs, dsMap);
            Long totalCount = datasetDataManage.datasetTotal(dto, replaceSql, null);
            totalCount = Objects.requireNonNullElse(totalCount, 0L);
            Long curLimit = ExportCenterUtils.getExportLimit("dataset");
            totalCount = totalCount > curLimit ? curLimit : totalCount;

            Long sheetCount = (totalCount / sheetLimit) + (totalCount % sheetLimit > 0 ? 1 : 0);
            CellStyle cellStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            cellStyle.setFont(font);
            cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (Long s = 1L; s < sheetCount + 1; s++) {
                Long sheetSize;
                if (s.equals(sheetCount)) {
                    sheetSize = totalCount - (s - 1) * sheetLimit;
                } else {
                    sheetSize = sheetLimit;
                }
                Long pageSize = (sheetSize / extractPageSize) + (sheetSize % extractPageSize > 0 ? 1 : 0);
                Sheet detailsSheet = null;
                List<List<String>> details = new ArrayList<>();
                for (Long p = 0L; p < pageSize; p++) {
                    int beforeCount = (int) ((s - 1) * sheetLimit);
                    String querySQL = SQLProvider.createQuerySQLWithLimit(sqlMeta, false, needOrder, false, beforeCount + p.intValue() * extractPageSize, extractPageSize);
                    if (pageSize == 1) {
                        querySQL = SQLProvider.createQuerySQLWithLimit(sqlMeta, false, needOrder, false, 0, sheetSize.intValue());
                    }
                    querySQL = provider.rebuildSQL(querySQL, sqlMeta, crossDs, dsMap);
                    DatasourceRequest datasourceRequest = new DatasourceRequest();
                    datasourceRequest.setQuery(querySQL);
                    datasourceRequest.setDsList(dsMap);
                    datasourceRequest.setIsCross(coreDatasetGroup.getIsCross());
                    Map<String, Object> previewData = datasetDataManage.buildPreviewData(provider.fetchResultField(datasourceRequest), allFields, desensitizationList, false);
                    List<Map<String, Object>> data = (List<Map<String, Object>>) previewData.get("data");
                    if (p.equals(0L)) {
                        detailsSheet = wb.createSheet("数据" + s);
                        List<String> header = new ArrayList<>();
                        for (DatasetTableFieldDTO field : allFields) {
                            header.add(field.getName());
                        }
                        details.add(header);
                        for (Map<String, Object> obj : data) {
                            List<String> row = new ArrayList<>();
                            for (DatasetTableFieldDTO field : allFields) {
                                String string = (String) obj.get(field.getEngineFieldName());
                                row.add(string);
                            }
                            details.add(row);
                        }
                        if (CollectionUtils.isNotEmpty(details)) {
                            for (int i = 0; i < details.size(); i++) {
                                Row row = detailsSheet.createRow(i);
                                List<String> rowData = details.get(i);
                                if (rowData != null) {
                                    for (int j = 0; j < rowData.size(); j++) {
                                        Cell cell = row.createCell(j);
                                        if (i == 0) {
                                            cell.setCellValue(rowData.get(j));
                                            cell.setCellStyle(cellStyle);
                                            detailsSheet.setColumnWidth(j, 255 * 20);
                                        } else {
                                            setExportCellValue(cell, allFields.get(j).getFieldType(), rowData.get(j));
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        details.clear();
                        for (Map<String, Object> obj : data) {
                            List<String> row = new ArrayList<>();
                            for (DatasetTableFieldDTO field : allFields) {
                                String string = (String) obj.get(field.getEngineFieldName());
                                row.add(string);
                            }
                            details.add(row);
                        }
                        int lastNum = detailsSheet.getLastRowNum();
                        for (int i = 0; i < details.size(); i++) {
                            Row row = detailsSheet.createRow(i + lastNum + 1);
                            List<String> rowData = details.get(i);
                            if (rowData != null) {
                                for (int j = 0; j < rowData.size(); j++) {
                                    Cell cell = row.createCell(j);
                                    if (i == 0) {
                                        cell.setCellValue(rowData.get(j));
                                        cell.setCellStyle(cellStyle);
                                        detailsSheet.setColumnWidth(j, 255 * 20);
                                    } else {
                                        setExportCellValue(cell, allFields.get(j).getFieldType(), rowData.get(j));
                                    }

                                }
                            }
                        }
                    }
                }
            }
            this.addWatermarkTools(wb);
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(request.getFilename(), StandardCharsets.UTF_8) + ".xlsx");
            wb.write(outputStream);
            outputStream.flush();
        } catch (RuntimeException e) {
            CrestException.throwException(e);
        }
    }
}
