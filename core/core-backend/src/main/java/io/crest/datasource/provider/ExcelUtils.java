package io.crest.datasource.provider;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crest.api.ds.vo.ExcelFileData;
import io.crest.api.ds.vo.ExcelSheetData;
import io.crest.api.ds.vo.RemoteExcelRequest;
import io.crest.commons.utils.EncryptUtils;
import io.crest.dataset.utils.FieldUtils;
import io.crest.dataset.utils.TableUtils;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.dto.TableField;
import io.crest.api.ds.vo.ExcelConfiguration;
import io.crest.i18n.Translator;
import io.crest.utils.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@SuppressWarnings({"deprecation", "unchecked"})
public class ExcelUtils {
    public static final String UFEFF = "\uFEFF";
    private static final int EXCEL_FIELD_DISPLAY_NAME_LIMIT = 100;
    private static final String DEFAULT_EXCEL_FIELD_TYPE = "TEXT";
    private static final String SHEET_STATUS_NORMAL = "NORMAL";
    private static final String SHEET_STATUS_SKIPPED = "SKIPPED";
    private static final String SHEET_MESSAGE_SQL_DESCRIPTION = "检测到该工作表首行疑似 SQL 或说明文本，默认不导入。";
    private static final String SHEET_MESSAGE_EMPTY_HEADER = "未检测到有效表头，默认不导入。";
    private static final String SHEET_MESSAGE_BLANK_HEADER = "检测到空表头，默认不导入。";
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String getExcelPath() {
        String configuredPath = ConfigUtils.getConfig("crest.path.excel", "/opt/crest/data/excel/");
        if (StringUtils.isBlank(configuredPath)) {
            configuredPath = "/opt/crest/data/excel/";
        }
        return configuredPath.endsWith(File.separator) ? configuredPath : configuredPath + File.separator;
    }

    private static File excelTempFile(String filename) {
        try {
            return FileUtils.resolveUnderDirectory(getExcelPath(), filename);
        } catch (IOException e) {
            CrestException.throwException(Translator.get("i18n_invalid_file_name"));
            return null;
        }
    }

    private static TypeReference<List<TableField>> TableFieldListTypeReference = new TypeReference<List<TableField>>() {
    };

    private static TypeReference<List<ExcelSheetData>> sheets = new TypeReference<List<ExcelSheetData>>() {
    };

    static String excelDbFieldName(String header) {
        return TableUtils.fieldNameShort(StringUtils.defaultString(header).trim());
    }

    private List<TableField> excelTableFields(List<String> headers) {
        List<TableField> fields = new ArrayList<>();
        Map<String, Integer> displayNameCounts = new HashMap<>();
        Set<String> usedDisplayNames = new HashSet<>();
        Set<String> usedDbFieldNames = new HashSet<>();
        for (String header : headers) {
            fields.add(excelTableField(uniqueExcelDisplayName(normalizeExcelHeader(header), displayNameCounts, usedDisplayNames), usedDbFieldNames));
        }
        return fields;
    }

    private TableField excelTableField(String displayName, Set<String> usedDbFieldNames) {
        TableField tableField = new TableField();
        tableField.setName(displayName);
        tableField.setOriginName(displayName);
        tableField.setDbFieldName(uniqueExcelDbFieldName(displayName, usedDbFieldNames));
        return tableField;
    }

    private String uniqueExcelDisplayName(String displayName, Map<String, Integer> displayNameCounts, Set<String> usedDisplayNames) {
        int count = displayNameCounts.getOrDefault(displayName, 0) + 1;
        displayNameCounts.put(displayName, count);
        String candidate = count == 1 ? displayName : excelDuplicateDisplayName(displayName, count);
        while (!usedDisplayNames.add(candidate)) {
            count = displayNameCounts.getOrDefault(displayName, count) + 1;
            displayNameCounts.put(displayName, count);
            candidate = excelDuplicateDisplayName(displayName, count);
        }
        return candidate;
    }

    private String excelDuplicateDisplayName(String displayName, int count) {
        String suffix = "_" + count;
        if (displayName.length() + suffix.length() <= EXCEL_FIELD_DISPLAY_NAME_LIMIT) {
            return displayName + suffix;
        }
        return displayName.substring(0, EXCEL_FIELD_DISPLAY_NAME_LIMIT - suffix.length()) + suffix;
    }

    private String uniqueExcelDbFieldName(String displayName, Set<String> usedDbFieldNames) {
        String dbFieldName = excelDbFieldName(displayName);
        int count = 2;
        while (!usedDbFieldNames.add(dbFieldName)) {
            dbFieldName = excelDbFieldName(displayName + "_" + count);
            count++;
        }
        return dbFieldName;
    }

    private static void normalizeExcelTableField(TableField field) {
        String type = StringUtils.firstNonBlank(field.getType(), field.getNativeType(), DEFAULT_EXCEL_FIELD_TYPE);
        field.setType(type);
        if (StringUtils.isBlank(field.getNativeType())) {
            field.setNativeType(type);
        }
        int fieldType = FieldUtils.resolveFieldType(type);
        if (field.getFieldType() == null) {
            field.setFieldType(fieldType);
        }
        if (field.getExtractedFieldType() == null) {
            field.setExtractedFieldType(fieldType);
        }
    }

    private String normalizeExcelHeader(String header) {
        String displayName = StringUtils.trim(header);
        if (StringUtils.isEmpty(displayName)) {
            CrestException.throwException(Translator.get("i18n_excel_error_first_row"));
        }
        if (displayName.length() > EXCEL_FIELD_DISPLAY_NAME_LIMIT) {
            CrestException.throwException("Excel 表头不能超过 100 个字符：" + StringUtils.abbreviate(displayName, 30));
        }
        return displayName;
    }

    private ExcelSheetInspection inspectSheet(List<String> header) {
        if (CollectionUtils.isEmpty(header)) {
            return ExcelSheetInspection.skipped(SHEET_MESSAGE_EMPTY_HEADER);
        }
        if (header.stream().allMatch(StringUtils::isBlank)) {
            return ExcelSheetInspection.skipped(SHEET_MESSAGE_EMPTY_HEADER);
        }
        if (header.stream().anyMatch(StringUtils::isBlank)) {
            return ExcelSheetInspection.skipped(SHEET_MESSAGE_BLANK_HEADER);
        }
        if (isSqlDescriptionSheet(header)) {
            return ExcelSheetInspection.skipped(SHEET_MESSAGE_SQL_DESCRIPTION);
        }
        return ExcelSheetInspection.normal();
    }

    private boolean isSqlDescriptionSheet(List<String> header) {
        List<String> cells = header.stream()
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toList());
        if (cells.isEmpty() || cells.size() > 2) {
            return false;
        }
        String firstRowText = String.join(" ", cells).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        int sqlScore = 0;
        String[] keywords = {"select ", " from ", " where ", " join ", " group by ", " order by ", " insert ", " update ", " delete ", " create ", " alter ", " with "};
        for (String keyword : keywords) {
            if (firstRowText.contains(keyword)) {
                sqlScore++;
            }
        }
        boolean startsWithSql = firstRowText.startsWith("select ")
                || firstRowText.startsWith("with ")
                || firstRowText.startsWith("insert ")
                || firstRowText.startsWith("update ")
                || firstRowText.startsWith("delete ")
                || firstRowText.startsWith("create ")
                || firstRowText.startsWith("alter ");
        return sqlScore >= 2 && (startsWithSql || firstRowText.length() > 80);
    }

    public static void mergeSheets(CoreDatasource requestDatasource, DatasourceDTO sourceData) {
        if (requestDatasource.getType().equalsIgnoreCase("Excel")) {
            List<ExcelSheetData> newSheets = JsonUtil.parseList(requestDatasource.getConfiguration(), sheets);
            List<String> tableNames = newSheets.stream().map(ExcelSheetData::getDisplayTableName).collect(Collectors.toList());
            List<ExcelSheetData> oldSheets = JsonUtil.parseList(sourceData.getConfiguration(), sheets);
            for (ExcelSheetData oldSheet : oldSheets) {
                if (!tableNames.contains(oldSheet.getDisplayTableName())) {
                    newSheets.add(oldSheet);
                }
            }
            requestDatasource.setConfiguration(JsonUtil.toJSONString(newSheets).toString());
        } else {
            ExcelConfiguration excelConfiguration = JsonUtil.parseObject(requestDatasource.getConfiguration(), ExcelConfiguration.class);
            List<ExcelSheetData> newSheets = excelConfiguration.getSheets();
            List<String> tableNames = newSheets.stream().map(ExcelSheetData::getDisplayTableName).collect(Collectors.toList());
            List<ExcelSheetData> oldSheets = JsonUtil.parseObject(sourceData.getConfiguration(), ExcelConfiguration.class).getSheets();
            for (ExcelSheetData oldSheet : oldSheets) {
                if (!tableNames.contains(oldSheet.getDisplayTableName())) {
                    newSheets.add(oldSheet);
                }
            }
            excelConfiguration.setSheets(newSheets);
            requestDatasource.setConfiguration(JsonUtil.toJSONString(excelConfiguration).toString());
        }

    }

    public static List<DatasetTableDTO> tables(DatasourceRequest datasourceRequest) throws CrestException {
        List<DatasetTableDTO> tableDescs = new ArrayList<>();
        try {
            String sheets = "";
            if (datasourceRequest.getDatasource().getType().equalsIgnoreCase("Excel")) {
                sheets = datasourceRequest.getDatasource().getConfiguration();
            } else {
                sheets = objectMapper.readTree(datasourceRequest.getDatasource().getConfiguration()).get("sheets").toString();
            }
            JsonNode rootNode = objectMapper.readTree(sheets);
            for (int i = 0; i < rootNode.size(); i++) {
                if (!isImportableSheet(rootNode.get(i))) {
                    continue;
                }
                DatasetTableDTO datasetTableDTO = new DatasetTableDTO();
                datasetTableDTO.setTableName(rootNode.get(i).get("displayTableName").asText());
                datasetTableDTO.setName(rootNode.get(i).get("displayTableName").asText());
                datasetTableDTO.setDatasourceId(datasourceRequest.getDatasource().getId());
                datasetTableDTO.setLastUpdateTime(rootNode.get(i).get("lastUpdateTime") == null ? datasourceRequest.getDatasource().getCreateTime() : rootNode.get(i).get("lastUpdateTime").asLong(0L));
                tableDescs.add(datasetTableDTO);
            }
        } catch (Exception e) {
            CrestException.throwException(e);
        }

        return tableDescs;
    }

    public static Map<String, String> getTableNamesMap(String type, String configuration) throws CrestException {
        Map<String, String> result = new HashMap<>();
        JsonNode rootNode = null;
        // 兼容历史未加密信息
        String sheets = configuration;
        try {
            if (type.equalsIgnoreCase("ExcelRemote")) {
                sheets = objectMapper.readTree(configuration).get("sheets").toString();
            }
            rootNode = objectMapper.readTree((String) EncryptUtils.aesDecrypt(sheets));
        } catch (Exception e) {
            try {
                rootNode = objectMapper.readTree(sheets);
            } catch (Exception ex) {
                CrestException.throwException(ex);
            }
        }
        if (rootNode != null) {
            for (int i = 0; i < rootNode.size(); i++) {
                if (!isImportableSheet(rootNode.get(i))) {
                    continue;
                }
                result.put(rootNode.get(i).get("tableName").asText(), rootNode.get(i).get("displayTableName").asText());
            }
        }
        return result;
    }

    private static boolean isImportableSheet(JsonNode sheetNode) {
        return sheetNode == null || sheetNode.get("sheet") == null || sheetNode.get("sheet").asBoolean(true);
    }

    public static String getFileName(CoreDatasource datasource) throws CrestException {
        if (datasource.getType().equalsIgnoreCase("ExcelRemote")) {
            ExcelConfiguration excelConfiguration = JsonUtil.parseObject(datasource.getConfiguration(), ExcelConfiguration.class);
            for (ExcelSheetData sheet : excelConfiguration.getSheets()) {
                return sheet.getFileName();
            }
        }
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree((String) EncryptUtils.aesDecrypt(datasource.getConfiguration()));
        } catch (Exception e) {
            try {
                rootNode = objectMapper.readTree(datasource.getConfiguration());
            } catch (Exception ex) {
                CrestException.throwException(ex);
            }
        }
        if (rootNode != null) {
            for (int i = 0; i < rootNode.size(); i++) {
                return rootNode.get(i).get("fileName").asText();
            }
        }
        return "";
    }

    public static String getSize(CoreDatasource datasource) throws CrestException {
        if (datasource.getType().equalsIgnoreCase("ExcelRemote")) {
            ExcelConfiguration excelConfiguration = JsonUtil.parseObject(datasource.getConfiguration(), ExcelConfiguration.class);
            for (ExcelSheetData sheet : excelConfiguration.getSheets()) {
                return sheet.getSize();
            }
        }
        try {
            JsonNode rootNode = objectMapper.readTree(datasource.getConfiguration());
            for (int i = 0; i < rootNode.size(); i++) {
                return rootNode.get(i).get("size").asText();
            }
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        return "0 B";
    }

    public List<String[]> fetchDataList(DatasourceRequest datasourceRequest) throws CrestException, IOException {
        List<String[]> dataList = new ArrayList<>();
        if (datasourceRequest.getDatasource().getType().equalsIgnoreCase("ExcelRemote")) {
            ExcelConfiguration excelConfiguration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), ExcelConfiguration.class);
            Map<String, String> fileNames = downLoadRemoteExcel(excelConfiguration);
            for (ExcelSheetData sheet : excelConfiguration.getSheets()) {
                if (sheet.getDisplayTableName().equalsIgnoreCase(datasourceRequest.getTable())) {
                    List<TableField> tableFields = sheet.getFields();
                    String suffix = fileNames.get("fileName").substring(fileNames.get("fileName").lastIndexOf(".") + 1);
                    try (InputStream inputStream = new FileInputStream(excelTempFile(fileNames.get("tranName")))) {
                        if (Strings.CI.equals(suffix, "csv")) {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                                reader.readLine();//去掉表头
                                dataList = csvData(reader, false, tableFields.size());
                            }
                        } else {
                            dataList = fetchExcelDataList(sheet.getTableName(), inputStream);
                        }
                    }
                }
            }
            if (StringUtils.isNotEmpty(fileNames.get("tranName"))) {
                FileUtils.deleteFile(excelTempFile(fileNames.get("tranName")).getPath());
            }
        } else {
            try {
                JsonNode rootNode = objectMapper.readTree(datasourceRequest.getDatasource().getConfiguration());
                for (int i = 0; i < rootNode.size(); i++) {
                    if (rootNode.get(i).get("displayTableName").asText().equalsIgnoreCase(datasourceRequest.getTable())) {
                        List<TableField> tableFields = JsonUtil.parseList(rootNode.get(i).get("fields").toString(), TableFieldListTypeReference);
                        String suffix = rootNode.get(i).get("path").asText().substring(rootNode.get(i).get("path").asText().lastIndexOf(".") + 1);
                        try (InputStream inputStream = new FileInputStream(rootNode.get(i).get("path").asText())) {
                            if (Strings.CI.equals(suffix, "csv")) {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                                    reader.readLine();//去掉表头
                                    dataList = csvData(reader, false, tableFields.size());
                                }
                            } else {
                                dataList = fetchExcelDataList(rootNode.get(i).get("tableName").asText(), inputStream);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                CrestException.throwException(e);
            }
        }
        return dataList;
    }

    private List<String[]> fetchExcelDataList(String sheetName, InputStream inputStream) {
        NoModelDataListener noModelDataListener = new NoModelDataListener();
        ExcelReader excelReader = EasyExcel.read(inputStream, noModelDataListener).build();
        List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
        for (ReadSheet readSheet : sheets) {
            if (!sheetName.equalsIgnoreCase(readSheet.getSheetName())) {
                continue;
            }
            noModelDataListener.clear();
            List<TableField> fields = new ArrayList<>();
            excelReader.read(readSheet);
            for (TableField tableFiled : excelTableFields(noModelDataListener.getHeader())) {
                tableFiled.setNativeType("TEXT");
                fields.add(tableFiled);
            }
        }
        return noModelDataListener.getData();
    }

    public static List<TableField> getTableFields(DatasourceRequest datasourceRequest) throws CrestException {
        List<TableField> tableFields = new ArrayList<>();
        TypeReference<List<TableField>> listTypeReference = new TypeReference<List<TableField>>() {
        };
        try {
            String sheets = "";
            if (datasourceRequest.getDatasource().getType().equalsIgnoreCase("Excel")) {
                sheets = datasourceRequest.getDatasource().getConfiguration();
            } else {
                sheets = objectMapper.readTree(datasourceRequest.getDatasource().getConfiguration()).get("sheets").toString();
            }
            JsonNode rootNode = objectMapper.readTree(sheets);
            for (int i = 0; i < rootNode.size(); i++) {
                if (rootNode.get(i).get("displayTableName").asText().equalsIgnoreCase(datasourceRequest.getTable())) {
                    tableFields = JsonUtil.parseList(rootNode.get(i).get("fields").toString(), listTypeReference);
                    tableFields.forEach(ExcelUtils::normalizeExcelTableField);
                }
            }
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        return tableFields;
    }

    public ExcelFileData excelSaveAndParse(MultipartFile file, String createBy) throws CrestException {
        String filename = file.getOriginalFilename();
        FileUtils.validateUploadFilename(filename);
        List<ExcelSheetData> excelSheetDataList = null;
        try {
            excelSheetDataList = parseExcel(filename, file.getInputStream(), true, filename);
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        List<ExcelSheetData> returnSheetDataList = new ArrayList<>();
        returnSheetDataList = excelSheetDataList;
        // save file
        String excelId = UUID.randomUUID().toString();
        String filePath = saveFile(file, excelId);

        for (ExcelSheetData excelSheetData : returnSheetDataList) {
            excelSheetData.setLastUpdateTime(System.currentTimeMillis());
            excelSheetData.setTableName(excelSheetData.getExcelLabel());
            excelSheetData.setDisplayTableName("excel_" + excelSheetData.getExcelLabel() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            excelSheetData.setPath(filePath);
            excelSheetData.setSheetId(UUID.randomUUID().toString());
            excelSheetData.setSheetExcelId(excelId);
            excelSheetData.setFileName(filename);
            for (TableField field : excelSheetData.getFields()) {
                normalizeExcelTableField(field);
            }
            long size = 0;
            String unit = "B";
            if (file.getSize() / 1024 == 0) {
                size = file.getSize();
            }
            if (0 < file.getSize() / 1024 && file.getSize() / 1024 < 1024) {
                size = file.getSize() / 1024;
                unit = "KB";
            }
            if (1024 <= file.getSize() / 1024) {
                size = file.getSize() / 1024 / 1024;
                unit = "MB";
            }
            excelSheetData.setSize(size + " " + unit);
        }
        ExcelFileData excelFileData = new ExcelFileData();
        excelFileData.setExcelLabel(filename.substring(0, filename.lastIndexOf('.')));
        excelFileData.setId(excelId);
        excelFileData.setPath(filePath);
        excelFileData.setSheets(returnSheetDataList);
        return excelFileData;
    }

    public static String checkStatus(DatasourceRequest datasourceRequest) throws FileNotFoundException {
        ExcelConfiguration excelConfiguration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), ExcelConfiguration.class);
        Map<String, String> fileNames = new HashMap<>();
        try {
            fileNames = downLoadRemoteExcel(excelConfiguration);
            return "Success";
        } catch (Exception e) {
            throw e;
        } finally {
            if (StringUtils.isNotEmpty(fileNames.get("tranName"))) {
                FileUtils.deleteFile(excelTempFile(fileNames.get("tranName")).getPath());
            }
        }
    }

    @SuppressWarnings("java/path-injection")
    public ExcelFileData parseRemoteExcel(RemoteExcelRequest remoteExcelRequest) throws CrestException, FileNotFoundException {
        Map<String, String> fileNames = downLoadRemoteExcel(remoteExcelRequest);
        List<ExcelSheetData> returnSheetDataList = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(excelTempFile(fileNames.get("tranName")))) {
            returnSheetDataList = parseExcel(fileNames.get("tranName"), fileInputStream, true, fileNames.get("fileName"));
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        for (ExcelSheetData excelSheetData : returnSheetDataList) {
            excelSheetData.setLastUpdateTime(System.currentTimeMillis());
            excelSheetData.setTableName(excelSheetData.getExcelLabel());
            excelSheetData.setDisplayTableName("excel_" + excelSheetData.getExcelLabel() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            excelSheetData.setPath(getExcelPath() + fileNames.get("tranName"));
            excelSheetData.setSheetId(UUID.randomUUID().toString());
            excelSheetData.setSheetExcelId(fileNames.get("tranName").split("\\.")[0]);
            excelSheetData.setFileName(fileNames.get("fileName"));
            for (TableField field : excelSheetData.getFields()) {
                normalizeExcelTableField(field);
            }
            long size = 0;
            File file = excelTempFile(fileNames.get("tranName"));
            String unit = "B";
            if (file.length() / 1024 == 0) {
                size = file.length();
            }
            if (0 < file.length() / 1024 && file.length() / 1024 < 1024) {
                size = file.length() / 1024;
                unit = "KB";
            }
            if (1024 <= file.length() / 1024) {
                size = file.length() / 1024 / 1024;
                unit = "MB";
            }
            excelSheetData.setSize(size + " " + unit);
        }

        ExcelFileData excelFileData = new ExcelFileData();
        excelFileData.setExcelLabel(fileNames.get("fileName").split("\\.")[0]);
        excelFileData.setId(fileNames.get("tranName").split("\\.")[0]);
        excelFileData.setPath(excelTempFile(fileNames.get("tranName")).getPath());
        excelFileData.setSheets(returnSheetDataList);
        if (StringUtils.isNotEmpty(fileNames.get("tranName"))) {
            FileUtils.deleteFile(excelTempFile(fileNames.get("tranName")).getPath());
        }
        return excelFileData;
    }

    private static Map<String, String> downLoadRemoteExcel(ExcelConfiguration remoteExcelRequest) throws CrestException, FileNotFoundException {
        Map<String, String> fileNames = new HashMap<>();
        if (remoteExcelRequest.getUrl().trim().startsWith("http")) {
            HttpClientConfig httpClientConfig = new HttpClientConfig();
            if (StringUtils.isNotEmpty(remoteExcelRequest.getUserName()) && StringUtils.isNotEmpty(remoteExcelRequest.getPasswd())) {
                String authValue = "Basic " + Base64.getUrlEncoder().encodeToString((remoteExcelRequest.getUserName() + ":" + remoteExcelRequest.getPasswd()).getBytes());
                httpClientConfig.addHeader("Authorization", authValue);
            }
            ensureExcelDirectory();
            fileNames = HttpClientUtil.downloadFile(remoteExcelRequest.getUrl(), httpClientConfig, getExcelPath());
        } else if (remoteExcelRequest.getUrl().trim().startsWith("ftp")) {
            fileNames = downLoadFromFtp(remoteExcelRequest);
        } else {
            CrestException.throwException(Translator.get("i18n_unsupported_protocol"));
        }
        return fileNames;
    }

    private static File ensureExcelDirectory() {
        try {
            return FileUtils.ensureDirectory(getExcelPath());
        } catch (IOException e) {
            CrestException.throwException("Excel文件目录创建失败: " + getExcelPath());
            return null;
        }
    }

    private static String saveFile(MultipartFile file, String fileNameUUID) throws CrestException {
        String filePath = null;
        try {
            String filename = file.getOriginalFilename();
            FileUtils.validateUploadFilename(filename);
            String suffix = filename.substring(filename.lastIndexOf(".") + 1);
            ensureExcelDirectory();
            File f = excelTempFile(fileNameUUID + "." + suffix);
            filePath = f.getPath();
            try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
                fileOutputStream.write(file.getBytes());
                fileOutputStream.flush();
            }
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        return filePath;
    }

    private static boolean isEmpty(List<String> cells) {
        if (CollectionUtils.isEmpty(cells)) {
            return true;
        }
        boolean isEmpty = true;
        for (int i = 0; i < cells.size(); i++) {
            if (isEmpty && StringUtils.isEmpty(cells.get(i))) {
                isEmpty = true;
            } else {
                isEmpty = false;
            }
        }
        return isEmpty;
    }

    public static List<String[]> csvData(BufferedReader reader, boolean isPreview, int size) throws CrestException {
        List<String[]> data = new ArrayList<>();
        try {
            int num = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> cells = parseCsvLine(line);
                if (!isEmpty(cells)) {
                    if (cells.size() > size) {
                        cells = cells.subList(0, size);
                    }
                    cells = cells.stream().map(ExcelUtils::normalizeNullPlaceholder).collect(Collectors.toList());
                    data.add(cells.toArray(new String[]{}));
                    num++;
                }
            }
        } catch (Exception e) {
            CrestException.throwException(e);
        }
        return data;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(current);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private String cellType(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (StringUtils.isEmpty(text) || text.length() > 19 || hasLeadingZeroNumericText(text)) {
            return "TEXT";
        }
        String regex = "^-?\\d+(\\.\\d+)?$";
        if (!text.matches(regex)) {
            return "TEXT";
        }
        try {
            Double d = Double.valueOf(text);
            double eps = 1e-10;
            if (d - Math.floor(d) < eps) {
                if (text.contains(".")) {
                    return "DOUBLE";
                }
                return "LONG";
            } else {
                return "DOUBLE";
            }
        } catch (Exception e2) {
            return "TEXT";
        }
    }

    private void cellType(String value, int i, TableField tableFiled) {
        if (StringUtils.isBlank(value) || isNullPlaceholder(value)) {
            return;
        }
        if (shouldKeepExcelFieldAsText(tableFiled)) {
            tableFiled.setNativeType(DEFAULT_EXCEL_FIELD_TYPE);
            return;
        }
        String type = cellType(value);
        if (shouldPreferExcelFieldAsDouble(tableFiled) && isExcelNumericType(type)) {
            type = "DOUBLE";
        }
        if (i == 0) {
            tableFiled.setNativeType(type);
        } else {
            if (tableFiled.getNativeType() == null) {
                tableFiled.setNativeType(type);
            } else {
                if (type.equalsIgnoreCase("TEXT")) {
                    tableFiled.setNativeType(type);
                }
                if (type.equalsIgnoreCase("DOUBLE") && tableFiled.getNativeType().equalsIgnoreCase("LONG")) {
                    tableFiled.setNativeType(type);
                }
            }
        }

    }

    private boolean shouldPreferExcelFieldAsDouble(TableField tableField) {
        if (tableField == null) {
            return false;
        }
        String fieldName = StringUtils.defaultString(StringUtils.firstNonBlank(tableField.getName(), tableField.getOriginName()));
        return containsAny(fieldName, "工作量", "金额", "均值", "平均", "阈值");
    }

    private boolean isExcelNumericType(String type) {
        return Strings.CI.equals(type, "LONG") || Strings.CI.equals(type, "DOUBLE");
    }

    private boolean hasLeadingZeroNumericText(String value) {
        String text = StringUtils.trimToEmpty(value);
        int start = text.startsWith("-") ? 1 : 0;
        return text.length() > start + 1 && text.charAt(start) == '0' && Character.isDigit(text.charAt(start + 1));
    }

    private boolean shouldKeepExcelFieldAsText(TableField tableField) {
        if (tableField == null) {
            return false;
        }
        String fieldName = StringUtils.defaultString(StringUtils.firstNonBlank(tableField.getName(), tableField.getOriginName()));
        String lowerName = fieldName.toLowerCase(Locale.ROOT);
        return containsAny(fieldName, "编号", "编码", "代码", "单号", "账号", "帐号", "卡号", "身份证", "版本号", "序号")
                || containsAny(fieldName, "日期", "时间", "月份", "年月")
                || hasEnglishIdentifierToken(lowerName, "id")
                || hasEnglishIdentifierToken(lowerName, "code");
    }

    private boolean containsAny(String value, String... keywords) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnglishIdentifierToken(String lowerName, String token) {
        if (StringUtils.isBlank(lowerName)) {
            return false;
        }
        if (lowerName.equals(token)) {
            return true;
        }
        return lowerName.matches(".*(^|[^a-z0-9])" + token + "($|[^a-z0-9]).*")
                || lowerName.matches(".*[\\u4e00-\\u9fa5]" + token + "$");
    }

    private static String normalizeExcelCellValue(String value) {
        if (StringUtils.isEmpty(value) || isNullPlaceholder(value)) {
            return null;
        }
        return value;
    }

    private static String normalizeNullPlaceholder(String value) {
        if (isNullPlaceholder(value)) {
            return null;
        }
        return value;
    }

    private static boolean isNullPlaceholder(String value) {
        String text = StringUtils.trimToEmpty(value);
        return Strings.CI.equals(text, "null") || Strings.CS.equals(text, "\\N");
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    public class NoModelDataListener extends AnalysisEventListener<Map<Integer, String>> {
        private List<String[]> data = new ArrayList<>();
        private List<String> header = new ArrayList<>();
        private List<Integer> headerKey = new ArrayList<>();

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            super.invokeHead(headMap, context);
            if (headMap.isEmpty()) {
                return;
            }
            int minKey = Collections.min(headMap.keySet());
            int maxKey = Collections.max(headMap.keySet());
            for (int key = minKey; key <= maxKey; key++) {
                String value = headCellValue(headMap.get(key));
                headerKey.add(key);
                header.add(value);
            }
        }

        private String headCellValue(ReadCellData<?> cellData) {
            if (cellData == null) {
                return null;
            }
            if (cellData.getType().equals(CellDataTypeEnum.STRING)) {
                return cellData.getStringValue();
            }
            if (cellData.getType().equals(CellDataTypeEnum.NUMBER)) {
                return cellData.getNumberValue().toString();
            }
            return null;
        }

        @Override
        public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
            List<String> line = new ArrayList<>();
            for (Integer key : headerKey) {
                line.add(normalizeExcelCellValue(dataMap.get(key)));
            }
            data.add(line.toArray(new String[line.size()]));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }

        public void clear() {
            data.clear();
            header.clear();
            headerKey.clear();
        }
    }

    @Data
    private static class ExcelSheetInspection {
        private boolean importable;
        private String status;
        private String message;

        private static ExcelSheetInspection normal() {
            ExcelSheetInspection inspection = new ExcelSheetInspection();
            inspection.setImportable(true);
            inspection.setStatus(SHEET_STATUS_NORMAL);
            inspection.setMessage("");
            return inspection;
        }

        private static ExcelSheetInspection skipped(String message) {
            ExcelSheetInspection inspection = new ExcelSheetInspection();
            inspection.setImportable(false);
            inspection.setStatus(SHEET_STATUS_SKIPPED);
            inspection.setMessage(message);
            return inspection;
        }
    }


    private List<ExcelSheetData> parseExcel(String filename, InputStream inputStream, boolean isPreview, String originFilename) throws IOException {
        List<ExcelSheetData> excelSheetDataList = new ArrayList<>();
        String suffix = filename.substring(filename.lastIndexOf(".") + 1);
        if (Strings.CI.equals(suffix, "xlsx") || Strings.CI.equals(suffix, "xls")) {
            NoModelDataListener noModelDataListener = new NoModelDataListener();
            ExcelReader excelReader = EasyExcel.read(inputStream, noModelDataListener).build();
            List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
            for (ReadSheet readSheet : sheets) {
                noModelDataListener.clear();
                List<TableField> fields = new ArrayList<>();
                excelReader.read(readSheet);
                List<String[]> data = new ArrayList<>(noModelDataListener.getData());
                ExcelSheetData excelSheetData = new ExcelSheetData();
                excelSheetData.setFileName(filename);
                excelSheetData.setExcelLabel(readSheet.getSheetName());
                ExcelSheetInspection inspection = inspectSheet(noModelDataListener.getHeader());
                excelSheetData.setSheet(inspection.isImportable());
                excelSheetData.setInspectionStatus(inspection.getStatus());
                excelSheetData.setInspectionMessage(inspection.getMessage());
                if (!inspection.isImportable()) {
                    excelSheetData.setFields(fields);
                    excelSheetData.setData(new ArrayList<>());
                    excelSheetDataList.add(excelSheetData);
                    continue;
                }
                for (TableField tableFiled : excelTableFields(noModelDataListener.getHeader())) {
                    tableFiled.setNativeType(null);
                    tableFiled.setChecked(true);
                    fields.add(tableFiled);
                }
                if (isPreview) {
                    for (int i = 0; i < data.size(); i++) {
                        for (int j = 0; j < data.get(i).length; j++) {
                            if (j < fields.size()) {
                                cellType(data.get(i)[j], i, fields.get(j));
                            }
                        }
                    }
                    if (data.size() > 100) {
                        data = data.subList(0, 100);
                    }
                }

                for (int i = 0; i < fields.size(); i++) {
                    if (StringUtils.isEmpty(fields.get(i).getNativeType())) {
                        fields.get(i).setNativeType(DEFAULT_EXCEL_FIELD_TYPE);
                    }
                    normalizeExcelTableField(fields.get(i));
                }

                excelSheetData.setFields(fields);
                excelSheetData.setData(data);
                excelSheetDataList.add(excelSheetData);
            }
        }

        if (Strings.CI.equals(suffix, "csv")) {
            List<TableField> fields = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String s = reader.readLine();// first line
            List<String> header = new ArrayList<>();
            if (StringUtils.isNotEmpty(s)) {
                List<String> split = parseCsvLine(s);
                for (String filedName : split) {
                    if (filedName.startsWith(UFEFF)) {
                        filedName = filedName.replace(UFEFF, "");
                    }
                    header.add(filedName);
                }
            }

            List<String[]> data = csvData(reader, isPreview, header.size());
            ExcelSheetInspection inspection = inspectSheet(header);
            if (inspection.isImportable()) {
                for (TableField tableFiled : excelTableFields(header)) {
                    tableFiled.setNativeType(null);
                    tableFiled.setChecked(true);
                    fields.add(tableFiled);
                }
            } else {
                data = new ArrayList<>();
            }
            if (isPreview) {
                for (int i = 0; i < data.size(); i++) {
                    for (int j = 0; j < data.get(i).length; j++) {
                        if (j < fields.size()) {
                            cellType(data.get(i)[j], i, fields.get(j));
                        }
                    }
                }
                if (data.size() > 100) {
                    data = data.subList(0, 100);
                }
            }
            for (int i = 0; i < fields.size(); i++) {
                if (StringUtils.isEmpty(fields.get(i).getNativeType())) {
                    fields.get(i).setNativeType(DEFAULT_EXCEL_FIELD_TYPE);
                }
                normalizeExcelTableField(fields.get(i));
            }

            ExcelSheetData excelSheetData = new ExcelSheetData();
            String[] fieldArray = fields.stream().map(TableField::getName).toArray(String[]::new);
            excelSheetData.setFields(fields);
            excelSheetData.setData(data);
            excelSheetData.setFileName(filename);
            excelSheetData.setExcelLabel(originFilename.substring(0, originFilename.lastIndexOf('.')));
            excelSheetData.setSheet(inspection.isImportable());
            excelSheetData.setInspectionStatus(inspection.getStatus());
            excelSheetData.setInspectionMessage(inspection.getMessage());
            excelSheetDataList.add(excelSheetData);
        }
        inputStream.close();

        for (ExcelSheetData excelSheetData : excelSheetDataList) {
            List<String[]> data = excelSheetData.getData();
            String[] fieldArray = excelSheetData.getFields().stream().map(TableField::getName).toArray(String[]::new);

            List<Map<String, Object>> jsonArray = new ArrayList<>();
            if (data != null) {
                jsonArray = data.stream().map(ele -> {
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 0; i < fieldArray.length; i++) {
                        map.put(fieldArray[i], i < ele.length ? ele[i] : "");
                    }
                    return map;
                }).collect(Collectors.toList());
            }
            excelSheetData.setJsonArray(jsonArray);
        }

        return excelSheetDataList;
    }

    @SuppressWarnings("java/ssrf")
    public static Map<String, String> downLoadFromFtp(ExcelConfiguration remoteExcelRequest) {
        Map<String, String> fileNames = new HashMap<>();
        String username = "";
        String password = "";
        String serverAddress = "";
        String filePath = "";
        try {
            URI uri = URI.create(remoteExcelRequest.getUrl());
            if (!Strings.CI.equals(uri.getScheme(), "ftp") || StringUtils.isBlank(uri.getHost())) {
                CrestException.throwException(Translator.get("i18n_invalid_address"));
            }
            serverAddress = uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
            filePath = StringUtils.defaultString(uri.getPath());
            if (StringUtils.isNotBlank(uri.getRawUserInfo())) {
                String userInfo = uri.getRawUserInfo();
                int separator = userInfo.indexOf(':');
                if (separator > -1) {
                    username = URLDecoder.decode(userInfo.substring(0, separator), StandardCharsets.UTF_8);
                    password = URLDecoder.decode(userInfo.substring(separator + 1), StandardCharsets.UTF_8);
                } else {
                    username = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            CrestException.throwException(Translator.get("i18n_invalid_address"));
        }
        if (StringUtils.isNotEmpty(remoteExcelRequest.getUserName())) {
            username = remoteExcelRequest.getUserName();
        }
        if (StringUtils.isNotEmpty(remoteExcelRequest.getPasswd())) {
            password = remoteExcelRequest.getPasswd();
        }
        filePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String suffix = filePath.substring(filePath.lastIndexOf(".") + 1);
        if (!Arrays.asList("csv", "xlsx", "xls").contains(suffix)) {
            CrestException.throwException(Translator.get("i18n_unsupported_file_format"));
        }
        String tranName = UUID.randomUUID().toString() + "." + suffix;
        String localFilePath = excelTempFile(tranName).getPath();
        fileNames.put("fileName", filePath);
        fileNames.put("tranName", tranName);

        try {
            URL url;
            HttpClientUtil.assertRemoteHostAllowed(serverAddress);
            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                url = new URL("ftp://" + username + ":" + URLEncoder.encode(password, StandardCharsets.UTF_8) + "@" + serverAddress + "/" + filePath);
            } else {
                url = new URL("ftp://" + serverAddress + "/" + filePath);
            }

            URLConnection conn = url.openConnection();
            try (InputStream inputStream = conn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(localFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

        } catch (IOException e) {
            CrestException.throwException(Translator.get("i18n_file_download_failed") + ", " + e.getMessage());
        }
        return fileNames;
    }

}
