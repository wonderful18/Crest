package io.crest.datasource.provider;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jcraft.jsch.*;
import io.crest.constant.SQLConstants;
import io.crest.dataset.utils.FieldUtils;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.entity.CoreDriver;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.crest.datasource.dao.auto.mapper.CoreDriverMapper;
import io.crest.datasource.manage.EngineManage;
import io.crest.datasource.request.EngineRequest;
import io.crest.datasource.security.SqlSecurityPolicy;
import io.crest.datasource.security.JdbcUrlSecurityPolicy;
import io.crest.datasource.type.*;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.*;
import io.crest.extensions.datasource.provider.DriverShim;
import io.crest.extensions.datasource.provider.CrestSqlFunctionTranslator;
import io.crest.extensions.datasource.provider.ExtendedJdbcClassLoader;
import io.crest.extensions.datasource.provider.Provider;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import io.crest.i18n.Translator;
import io.crest.utils.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.Lex;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Component("calciteProvider")
@SuppressWarnings({"deprecation", "unchecked"})
public class CalciteProvider extends Provider {

    @Resource
    protected CoreDatasourceMapper coreDatasourceMapper;
    @Resource
    protected CoreDriverMapper coreDriverMapper;
    @Resource
    private EngineManage engineManage;
    protected ExtendedJdbcClassLoader extendedJdbcClassLoader;
    private Map<Long, ExtendedJdbcClassLoader> customJdbcClassLoaders = new HashMap<>();
    @Value("${crest.path.driver:/opt/crest/drivers}")
    private String FILE_PATH;
    @Value("${crest.path.custom-drivers:/opt/crest/custom-drivers/}")
    private String CUSTOM_PATH;
    private static String split = "DE";

    @Resource
    private CommonThreadPool commonThreadPool;

    private boolean isOracleLike(String type) {
        return Strings.CI.equalsAny(type,
                DatasourceConfiguration.DatasourceType.oracle.getType(),
                DatasourceConfiguration.DatasourceType.obOracle.getType());
    }

    private boolean isObOracle(String type) {
        return Strings.CI.equals(type, DatasourceConfiguration.DatasourceType.obOracle.getType());
    }

    boolean shouldUseReadOnlyConnection(
            DatasourceRequest datasourceRequest,
            DatasourceConfiguration datasourceConfiguration,
            String datasourceType
    ) {
        if (Boolean.TRUE.equals(datasourceRequest.getReadOnly())) {
            return true;
        }
        return isObOracle(datasourceType)
                && datasourceConfiguration instanceof ObOracle obOracle
                && !Boolean.FALSE.equals(obOracle.getReadOnly());
    }

    private void setConnectionReadOnly(Connection connection, boolean readOnly) {
        if (!readOnly || connection == null) {
            return;
        }
        try {
            connection.setReadOnly(true);
        } catch (SQLException e) {
            CrestException.throwException("源库只读连接启用失败：" + e.getMessage());
        }
    }

    private void resetConnectionReadOnly(Connection connection, boolean readOnly) {
        if (!readOnly || connection == null) {
            return;
        }
        try {
            connection.setReadOnly(false);
        } catch (SQLException e) {
            LogUtil.warn("源库只读连接恢复失败：" + e.getMessage());
        }
    }

    private DatasourceConfiguration parseOracleLikeConfiguration(String configuration, String type) {
        if (isObOracle(type)) {
            return parseDatasourceConfiguration(configuration, ObOracle.class);
        }
        return parseDatasourceConfiguration(configuration, Oracle.class);
    }

    private DatasourceConfiguration parseDatasourceConfiguration(String configuration, String type) {
        if (isOracleLike(type)) {
            return parseOracleLikeConfiguration(configuration, type);
        }
        return parseDatasourceConfiguration(configuration, DatasourceConfiguration.class);
    }

    private <T extends DatasourceConfiguration> T parseDatasourceConfiguration(String configuration, Class<T> configurationClass) {
        String trimmedConfiguration = StringUtils.trimToEmpty(configuration);
        if (StringUtils.isBlank(trimmedConfiguration) || !trimmedConfiguration.startsWith("{")) {
            CrestException.throwException("数据源配置格式无效");
        }
        T parsedConfiguration = JsonUtil.parseObjectQuietly(trimmedConfiguration, configurationClass);
        if (parsedConfiguration == null) {
            CrestException.throwException("数据源配置格式无效");
        }
        return parsedConfiguration;
    }

    @PostConstruct
    public void init() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        extendedJdbcClassLoader = new ExtendedJdbcClassLoader(new URL[0], classLoader);
        try {
            File file = FileUtils.ensureDirectory(FILE_PATH);
            extendedJdbcClassLoader = new ExtendedJdbcClassLoader(new URL[]{file.toURI().toURL()}, classLoader);
            File[] array = file.listFiles();
            Optional.ofNullable(array).ifPresent(files -> {
                for (File tmp : array) {
                    if (tmp.getName().endsWith(".jar")) {
                        try {
                            extendedJdbcClassLoader.addFile(tmp);
                        } catch (IOException e) {
                            io.crest.utils.LogUtil.error(e.getMessage(), e);
                        }
                    }
                }
            });
        } catch (IOException | RuntimeException e) {
            LogUtil.info("数据源驱动目录不可用，将使用应用内置驱动");
        }
    }

    @Override
    public List<String> getSchema(DatasourceRequest datasourceRequest) {
        List<String> schemas = new ArrayList<>();
        String queryStr = getSchemaSql(datasourceRequest.getDatasource());
        try (ConnectionObj con = getConnection(datasourceRequest.getDatasource())) {
            DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(
                    datasourceRequest.getDatasource().getConfiguration(),
                    datasourceRequest.getDatasource().getType()
            );
            boolean readOnly = shouldUseReadOnlyConnection(
                    datasourceRequest,
                    datasourceConfiguration,
                    datasourceRequest.getDatasource().getType()
            );
            setConnectionReadOnly(con.getConnection(), readOnly);
            try (Statement statement = getStatement(con.getConnection(), 30);
                 ResultSet resultSet = statement.executeQuery(queryStr)) {
                while (resultSet.next()) {
                    schemas.add(resultSet.getString(1));
                }
            } finally {
                resetConnectionReadOnly(con.getConnection(), readOnly);
            }
        } catch (SQLException | RuntimeException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(e.getMessage());
        }
        if (datasourceRequest.getDatasource().getType().equalsIgnoreCase(DatasourceConfiguration.DatasourceType.pg.name())) {
            Set<String> SYSTEM_SCHEMAS = new HashSet<>(Arrays.asList("information_schema", "pg_catalog", "pg_temp_1", "pg_toast", "pg_toast_temp_1"));
            return schemas.stream().filter(schema -> !SYSTEM_SCHEMAS.contains(schema)).collect(Collectors.toList());
        }
        return schemas;
    }

    @Override
    public String checkStatus(DatasourceRequest datasourceRequest) {
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(datasourceRequest.getDatasource().getType());
        switch (datasourceType) {
            case pg:
                DatasourceConfiguration configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Pg.class);
                List<String> schemas = getSchema(datasourceRequest);
                if (CollectionUtils.isEmpty(schemas) || !schemas.contains(configuration.getSchema())) {
                    CrestException.throwException("无效的 schema！");
                }
                break;
            default:
                break;
        }

        try (ConnectionObj con = getConnection(datasourceRequest.getDatasource())) {
            DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(
                    datasourceRequest.getDatasource().getConfiguration(),
                    datasourceRequest.getDatasource().getType()
            );
            boolean readOnly = shouldUseReadOnlyConnection(
                    datasourceRequest,
                    datasourceConfiguration,
                    datasourceRequest.getDatasource().getType()
            );
            setConnectionReadOnly(con.getConnection(), readOnly);
            try {
                datasourceRequest.setDsVersion(con.getConnection().getMetaData().getDatabaseMajorVersion());
                QueryAndParams queryAndParams = tablesSql(datasourceRequest).get(0);
                try (PreparedStatement statement = con.getConnection().prepareStatement(queryAndParams.sql())) {
                    configureStatement(statement, queryAndParams, 30);
                    try (ResultSet ignored = statement.executeQuery()) {
                        // 验证连接和元数据查询能正常执行即可。
                    }
                }
            } finally {
                resetConnectionReadOnly(con.getConnection(), readOnly);
            }
        } catch (SQLException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        return "Success";
    }

    @Override
    public List<DatasetTableDTO> tables(DatasourceRequest datasourceRequest) {
        List<DatasetTableDTO> tables = new ArrayList<>();
        try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId())) {
            DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(
                    datasourceRequest.getDatasource().getConfiguration(),
                    datasourceRequest.getDatasource().getType()
            );
            boolean readOnly = shouldUseReadOnlyConnection(
                    datasourceRequest,
                    datasourceConfiguration,
                    datasourceRequest.getDatasource().getType()
            );
            setConnectionReadOnly(con, readOnly);
            try {
                datasourceRequest.setDsVersion(con.getMetaData().getDatabaseMajorVersion());
                List<QueryAndParams> tablesSqls = tablesSql(datasourceRequest);
                for (QueryAndParams tablesSql : tablesSqls) {
                    try (PreparedStatement statement = con.prepareStatement(tablesSql.sql())) {
                        configureStatement(statement, tablesSql, 30);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                                tables.add(getTableDesc(datasourceRequest, resultSet));
                            }
                        }
                    }
                }
            } finally {
                resetConnectionReadOnly(con, readOnly);
            }
        } catch (SQLException | RuntimeException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(e.getMessage());
        }
        return tables;
    }

    @Override
    @SuppressWarnings("java/sql-injection")
    public Map<String, Object> fetchResultField(DatasourceRequest datasourceRequest) throws CrestException {
        // 不跨数据源
        if (datasourceRequest.getIsCross() == null || !datasourceRequest.getIsCross()) {
            return jdbcFetchResultField(datasourceRequest);
        }

        List<TableField> datasetTableFields = new ArrayList<>();
        List<String[]> list = new LinkedList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = take();
        try {
            CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
            String readQuery = SqlSecurityPolicy.validateReadQuery(datasourceRequest.getQuery());
            statement = calciteConnection.prepareStatement(readQuery);
            bindPreparedStatementValues(statement, datasourceRequest.getTableFieldWithValues());
            resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                TableField tableField = new TableField();
                tableField.setOriginName(metaData.getColumnLabel(i));
                tableField.setType(metaData.getColumnTypeName(i));
                tableField.setPrecision(metaData.getPrecision(i));
                int fieldType = FieldUtils.resolveFieldType(tableField.getType());
                tableField.setExtractedFieldType(fieldType);
                tableField.setFieldType(fieldType);
                tableField.setScale(metaData.getScale(i));
                datasetTableFields.add(tableField);
            }
            list = dataResult(resultSet);
        } catch (SQLException | RuntimeException | AssertionError e) {
            String msg;
            if (e.getCause() != null && e.getCause().getCause() != null) {
                msg = e.getMessage() + " [" + e.getCause().getCause().getMessage() + "]";
            } else {
                msg = e.getMessage();
            }
            CrestException.throwException(Translator.get("i18n_fetch_error") + msg);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fields", datasetTableFields);
        map.put("data", list);
        return map;
    }

    @Override
    public String transSqlDialect(String sql, Map<Long, DatasourceSchemaDTO> dsMap) throws CrestException {
        DatasourceSchemaDTO value = dsMap.entrySet().iterator().next().getValue();
        try (Connection connection = getConnectionFromPool(value.getId());) {
            // 获取数据库version
            if (connection != null) {
                value.setDsVersion(connection.getMetaData().getDatabaseMajorVersion());
            }
            SqlParser parser = SqlParser.create(sql, SqlParser.Config.DEFAULT.withLex(Lex.JAVA));
            SqlNode sqlNode = parser.parseStmt();
            String dialect = restoreUnicodeStringLiterals(sqlNode.toSqlString(getDialect(value)).toString());
            return CrestSqlFunctionTranslator.translate(dialect, value);
        } catch (SQLException | SqlParseException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        return null;
    }

    private List<TableField> fetchResultField(ResultSet rs) throws SQLException {
        List<TableField> fieldList = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int j = 0; j < columnCount; j++) {
            String columnName = metaData.getColumnName(j + 1);
            String label = StringUtils.isNotEmpty(metaData.getColumnLabel(j + 1)) ? metaData.getColumnLabel(j + 1) : columnName;
            TableField tableField = new TableField();
            tableField.setOriginName(columnName);
            tableField.setName(label);
            tableField.setType(metaData.getColumnTypeName(j + 1));
            tableField.setNativeType(tableField.getType());
            int fieldType = FieldUtils.resolveFieldType(tableField.getType());
            tableField.setExtractedFieldType(fieldType);
            tableField.setFieldType(fieldType);
            fieldList.add(tableField);
        }
        return fieldList;
    }

    private Map<String, Integer> getTableTypeMap(DatasourceRequest datasourceRequest, DatasourceConfiguration datasourceConfiguration, String tableName) throws CrestException {
        Map<String, Integer> map = new HashMap<>();
        String schemaTable = (ObjectUtils.isNotEmpty(datasourceConfiguration.getSchema()) ? (quoteIdentifierPart(datasourceConfiguration.getSchema()) + ".") : "") + quoteIdentifierPart(tableName);
        // nosemgrep: java.lang.security.audit.formatted-sql-string.formatted-sql-string - SQL identifiers cannot be bound as parameters; each part is escaped by quoteIdentifierPart.
        String sql = "SELECT * FROM " + schemaTable + " LIMIT 0 OFFSET 0";
        sql = transSqlDialect(sql, datasourceRequest.getDsList());
        ResultSet resultSet = null;
        try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId());
             PreparedStatement statement = con.prepareStatement(sql)) {
            boolean readOnly = shouldUseReadOnlyConnection(
                    datasourceRequest,
                    datasourceConfiguration,
                    datasourceRequest.getDatasource().getType()
            );
            setConnectionReadOnly(con, readOnly);
            try {
                statement.setQueryTimeout(30);
                // nosemgrep: java.lang.security.audit.formatted-sql-string.formatted-sql-string - table identifiers were already quoted and escaped before preparing the statement.
                resultSet = statement.executeQuery();

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int j = 0; j < columnCount; j++) {
                    String name = StringUtils.lowerCase(metaData.getColumnName(j + 1));
                    Integer type = metaData.getColumnType(j + 1);
                    map.put(name, type);
                }
            } finally {
                resetConnectionReadOnly(con, readOnly);
            }
        } catch (SQLException | RuntimeException e) {
            io.crest.utils.LogUtil.error(e.getMessage(), e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    io.crest.utils.LogUtil.error(e.getMessage(), e);
                }
            }
        }
        return map;
    }

    private String quoteIdentifierPart(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            CrestException.throwException("Illegal empty identifier");
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    static String quoteOracleSchema(String schema) {
        if (StringUtils.isBlank(schema)) {
            CrestException.throwException(Translator.get("i18n_schema_is_empty"));
        }
        String value = schema.trim();
        if (value.indexOf('\0') >= 0 || value.contains(".") || value.length() > 128) {
            CrestException.throwException(Translator.get("i18n_invalid_ds"));
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String quoteQualifiedIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            CrestException.throwException("Illegal empty identifier");
        }
        StringBuilder result = new StringBuilder();
        int start = 0;
        while (start <= identifier.length()) {
            int end = identifier.indexOf('.', start);
            String part = end < 0 ? identifier.substring(start) : identifier.substring(start, end);
            if (result.length() > 0) {
                result.append('.');
            }
            result.append(quoteIdentifierPart(part));
            if (end < 0) {
                break;
            }
            start = end + 1;
        }
        return result.toString();
    }

    private String sqlLiteral(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "''") + "'";
    }

    private void bindPreparedStatementValues(PreparedStatement statement, List<TableFieldWithValue> tableFieldWithValues) throws SQLException {
        if (statement == null || CollectionUtils.isEmpty(tableFieldWithValues)) {
            return;
        }
        for (int i = 0; i < tableFieldWithValues.size(); i++) {
            TableFieldWithValue tableFieldWithValue = tableFieldWithValues.get(i);
            Object valueObject = tableFieldWithValue.getValue();
            if (Objects.equals(tableFieldWithValue.getType(), Types.CLOB) && valueObject instanceof String stringValue) {
                Reader reader = new StringReader(stringValue);
                statement.setCharacterStream(i + 1, reader, stringValue.length());
            } else if (tableFieldWithValue.getType() != null) {
                statement.setObject(i + 1, valueObject, tableFieldWithValue.getType());
            } else {
                statement.setObject(i + 1, valueObject);
            }
        }
    }

    @Override
    @SuppressWarnings("java/sql-injection")
    public List<TableField> fetchTableField(DatasourceRequest datasourceRequest) throws CrestException {
        if (datasourceRequest.getIsCross() != null && datasourceRequest.getIsCross()) {
            List<TableField> datasetTableFields = new ArrayList<>();
            try {
                Connection connection = take();
                CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
                String readQuery = SqlSecurityPolicy.validateReadQuery(datasourceRequest.getQuery());
                try (PreparedStatement statement = calciteConnection.prepareStatement(readQuery)) {
                    bindPreparedStatementValues(statement, datasourceRequest.getTableFieldWithValues());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            TableField tableField = new TableField();
                            tableField.setOriginName(metaData.getColumnLabel(i));
                            tableField.setType(metaData.getColumnTypeName(i));
                            tableField.setPrecision(metaData.getPrecision(i));
                            int fieldType = FieldUtils.resolveFieldType(tableField.getType());
                            tableField.setExtractedFieldType(fieldType);
                            tableField.setFieldType(fieldType);
                            tableField.setScale(metaData.getScale(i));
                            datasetTableFields.add(tableField);
                        }
                    }
                }
            } catch (SQLException | RuntimeException e) {
                throw CrestException.getException(e.getMessage());
            }
            return datasetTableFields;
        }
        List<TableField> datasetTableFields = new ArrayList<>();
        DatasourceSchemaDTO datasourceSchemaDTO = datasourceRequest.getDsList().entrySet().iterator().next().getValue();
        datasourceRequest.setDatasource(datasourceSchemaDTO);

        DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(datasourceRequest.getDatasource().getConfiguration(), datasourceSchemaDTO.getType());

        String table = datasourceRequest.getTable();
        if (StringUtils.isEmpty(table)) {
            ResultSet resultSet = null;
            try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId());
                 Statement statement = getStatement(con, 30)) {
                if (isOracleLike(datasourceSchemaDTO.getType())) {
                    statement.executeUpdate("ALTER SESSION SET CURRENT_SCHEMA = " + quoteOracleSchema(datasourceConfiguration.getSchema()));
                }
                boolean readOnly = shouldUseReadOnlyConnection(datasourceRequest, datasourceConfiguration, datasourceSchemaDTO.getType());
                setConnectionReadOnly(con, readOnly);
                try {
                    String readQuery = SqlSecurityPolicy.validateReadQuery(datasourceRequest.getQuery());
                    resultSet = statement.executeQuery(readQuery);
                    datasetTableFields.addAll(getField(resultSet, datasourceRequest));
                } finally {
                    resetConnectionReadOnly(con, readOnly);
                }
            } catch (SQLException | RuntimeException e) {
                CrestException.throwException(e.getMessage());
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        io.crest.utils.LogUtil.error(e.getMessage(), e);
                    }
                }
            }
        } else {
            if (!tables(datasourceRequest).stream().map(DatasetTableDTO::getTableName).collect(Collectors.toList()).contains(table)) {
                CrestException.throwException(Translator.get("i18n_invalid_table_name"));
            }
            ResultSet resultSet = null;
            try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId());
                 Statement statement = getStatement(con, 30)) {
                boolean readOnly = shouldUseReadOnlyConnection(datasourceRequest, datasourceConfiguration, datasourceSchemaDTO.getType());
                setConnectionReadOnly(con, readOnly);
                try {
                    datasourceRequest.setDsVersion(con.getMetaData().getDatabaseMajorVersion());
                    if (datasourceRequest.getDatasource().getType().equalsIgnoreCase("mongo")) {
                        String fieldSql = "select * from " + quoteQualifiedIdentifier(table) + " limit 0 offset 0 ";
                        resultSet = statement.executeQuery(fieldSql);
                        return fetchResultField(resultSet);
                    }
                    if (isDorisCatalog(datasourceRequest)) {
                        String fieldSql = "desc " + quoteQualifiedIdentifier(table);
                        resultSet = statement.executeQuery(fieldSql);
                    } else {
                        String fieldSql = getTableFiledSql(datasourceRequest);
                        resultSet = statement.executeQuery(fieldSql);
                    }

                    Map<String, Integer> tableTypeMap = getTableTypeMap(datasourceRequest, datasourceConfiguration, table);

                    while (resultSet.next()) {
                        TableField tableFieldDesc = getTableFieldDesc(datasourceRequest, resultSet, 3, tableTypeMap);
                        boolean repeat = false;
                        for (TableField ele : datasetTableFields) {
                            if (Strings.CI.equals(ele.getOriginName(), tableFieldDesc.getOriginName())) {
                                repeat = true;
                                break;
                            }
                        }
                        if (!repeat) {
                            datasetTableFields.add(tableFieldDesc);
                        }
                    }
                    if (isObOracle(datasourceRequest.getDatasource().getType())) {
                        DatasourceConfiguration configuration = parseOracleLikeConfiguration(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getDatasource().getType());
                        applyOracleColumnComments(datasetTableFields, getOracleColumnComments(con, configuration.getSchema(), table));
                    }
                } finally {
                    resetConnectionReadOnly(con, readOnly);
                }
            } catch (SQLException | RuntimeException e) {
                CrestException.throwException(e.getMessage());
            } finally {
                if (resultSet != null) {
                    try {
                        resultSet.close();
                    } catch (SQLException e) {
                        io.crest.utils.LogUtil.error(e.getMessage(), e);
                    }
                }
            }
        }

        return datasetTableFields;
    }

    private boolean isDorisCatalog(DatasourceRequest datasourceRequest) {
        if (!datasourceRequest.getDatasource().getType().equalsIgnoreCase("doris")) {
            return false;
        }
        DatasourceConfiguration configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Mysql.class);
        String database = "";
        if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
            database = configuration.getDataBase();
        } else {
            database = databaseFromJdbcUrl(configuration.getJdbcUrl());
        }
        return database.contains(".");
    }

    private String databaseFromJdbcUrl(String jdbcUrl) {
        if (StringUtils.isBlank(jdbcUrl)) {
            CrestException.throwException(Translator.get("i18n_invalid_connection"));
        }
        int schemeEnd = jdbcUrl.indexOf("://");
        int databaseStart = jdbcUrl.indexOf('/', schemeEnd < 0 ? 0 : schemeEnd + 3);
        if (databaseStart < 0 || databaseStart + 1 >= jdbcUrl.length()) {
            CrestException.throwException(Translator.get("i18n_invalid_connection"));
        }
        int queryStart = jdbcUrl.indexOf('?', databaseStart + 1);
        int semicolonStart = jdbcUrl.indexOf(';', databaseStart + 1);
        int databaseEnd = jdbcUrl.length();
        if (queryStart > -1) {
            databaseEnd = Math.min(databaseEnd, queryStart);
        }
        if (semicolonStart > -1) {
            databaseEnd = Math.min(databaseEnd, semicolonStart);
        }
        return jdbcUrl.substring(databaseStart + 1, databaseEnd);
    }

    @Override
    public ConnectionObj getConnection(DatasourceDTO coreDatasource) {
        ConnectionObj connectionObj = new ConnectionObj();
        DatasourceConfiguration configuration = null;
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(coreDatasource.getType());
        switch (datasourceType) {
            case mysql:
            case mongo:
            case StarRocks:
            case doris:
            case TiDB:
            case mariadb:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Mysql.class);
                break;
            case impala:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Impala.class);
                break;
            case sqlServer:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Sqlserver.class);
                break;
            case oracle:
            case obOracle:
                configuration = parseOracleLikeConfiguration(coreDatasource.getConfiguration(), coreDatasource.getType());
                break;
            case db2:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Db2.class);
                break;
            case pg:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Pg.class);
                break;
            case redshift:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Redshift.class);
                break;
            case h2:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), H2.class);
                break;
            case ck:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), CK.class);
                break;
            default:
                configuration = parseDatasourceConfiguration(coreDatasource.getConfiguration(), Mysql.class);
        }
        CoreDriver customDriver = resolveCustomDriver(coreDatasource.getType(), configuration.getCustomDriver());
        String driverClassName = JdbcUrlSecurityPolicy.resolveDriverClass(coreDatasource.getType(), configuration.getDriver(), configuration.getCustomDriver(), customDriver);
        configuration.setDriver(driverClassName);
        startSshSession(configuration, connectionObj, null);
        Properties props = new Properties();
        if (StringUtils.isNotBlank(configuration.getUsername())) {
            props.setProperty("user", configuration.getUsername());
        }
        if (StringUtils.isNotBlank(configuration.getPassword())) {
            props.setProperty("password", configuration.getPassword());
        }
        ExtendedJdbcClassLoader jdbcClassLoader = JdbcUrlSecurityPolicy.isDefaultCustomDriver(configuration.getCustomDriver()) ? extendedJdbcClassLoader : getCustomJdbcClassLoader(customDriver);
        Connection conn = null;
        try {
            Driver driverClass = (Driver) jdbcClassLoader.loadClass(driverClassName).newInstance();
            String jdbcUrl = JdbcUrlSecurityPolicy.validate(coreDatasource.getType(), configuration.getJdbc(), configuration.getExtraParams());
            conn = driverClass.connect(jdbcUrl, props);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        connectionObj.setConnection(conn);
        return connectionObj;
    }

    private CoreDriver resolveCustomDriver(String datasourceType, String customDriver) {
        if (JdbcUrlSecurityPolicy.isDefaultCustomDriver(customDriver)) {
            return null;
        }
        Long customDriverId;
        try {
            customDriverId = Long.valueOf(customDriver);
        } catch (NumberFormatException e) {
            CrestException.throwException("invalid driver");
            return null;
        }
        CoreDriver coreDriver = coreDriverMapper.selectById(customDriverId);
        if (coreDriver == null || !StringUtils.equalsIgnoreCase(coreDriver.getType(), datasourceType)) {
            CrestException.throwException("invalid driver");
        }
        return coreDriver;
    }

    private DatasetTableDTO getTableDesc(DatasourceRequest datasourceRequest, ResultSet resultSet) throws SQLException {
        DatasetTableDTO tableDesc = new DatasetTableDTO();
        tableDesc.setDatasourceId(datasourceRequest.getDatasource().getId());
        tableDesc.setType("db");
        tableDesc.setTableName(resultSet.getString(1));
        if (resultSet.getMetaData().getColumnCount() > 1) {
            tableDesc.setName(resultSet.getString(2));
        } else {
            tableDesc.setName(resultSet.getString(1));
        }
        return tableDesc;
    }

    private List<String> getDriver() {
        List<String> drivers = new ArrayList<>();
        Map<String, DatasourceConfiguration> beansOfType = CommonBeanFactory.getApplicationContext().getBeansOfType((DatasourceConfiguration.class));
        beansOfType.keySet().forEach(key -> drivers.add(beansOfType.get(key).getDriver()));
        return drivers;
    }

    @SuppressWarnings("java/sql-injection")
    public Map<String, Object> jdbcFetchResultField(DatasourceRequest datasourceRequest) throws CrestException {
        DatasourceSchemaDTO value = datasourceRequest.getDsList().entrySet().iterator().next().getValue();
        datasourceRequest.setDatasource(value);

        DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(datasourceRequest.getDatasource().getConfiguration(), value.getType());

        Map<String, Object> map = new LinkedHashMap<>();
        List<TableField> fieldList = new ArrayList<>();
        List<String[]> dataList = new LinkedList<>();

        // schema
        ResultSet resultSet = null;
        String oracleCharset = normalizeOracleCharset(datasourceConfiguration.getCharset());
        String oracleTargetCharset = normalizeOracleCharset(datasourceConfiguration.getTargetCharset());

        try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId())) {

            boolean readOnly = shouldUseReadOnlyConnection(datasourceRequest, datasourceConfiguration, value.getType());
            setConnectionReadOnly(con, readOnly);
            try (Statement statement = getStatement(value, con, datasourceRequest, datasourceConfiguration, null)) {
                if (CollectionUtils.isNotEmpty(datasourceRequest.getTableFieldWithValues())) {
                    LogUtil.info("execWithPreparedStatement uses parameterized SQL");
                    for (int i = 0; i < datasourceRequest.getTableFieldWithValues().size(); i++) {
                        try {
                            Object valueObject = datasourceRequest.getTableFieldWithValues().get(i).getValue();

                            if (valueObject instanceof String && isOracleLike(value.getType())) {
                                if (StringUtils.isNotEmpty(oracleCharset) && StringUtils.isNotEmpty(oracleTargetCharset)) {
                                    //转换为数据库的字符集
                                    valueObject = convertOracleText((String) valueObject, oracleTargetCharset, oracleCharset);
                                }
                                if (datasourceRequest.getTableFieldWithValues().get(i).getType().equals(Types.CLOB)) {
                                    Reader reader = new StringReader((String) valueObject);
                                    ((PreparedStatement) statement).setCharacterStream(i + 1, reader, ((String) valueObject).length());
                                } else {
                                    ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                                }
                            } else {
                                ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                            }
                            LogUtil.info("execWithPreparedStatement param[" + (i + 1) + "](" + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName() + ")");
                        } catch (SQLException e) {
                            throw new SQLException(e.getMessage() + ". TARGET TYPE: " + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName());
                        }
                    }
                    resultSet = ((PreparedStatement) statement).executeQuery();
                } else {
                    String readQuery = SqlSecurityPolicy.validateReadQuery(datasourceRequest.getQuery());
                    resultSet = statement.executeQuery(readQuery);
                }
                fieldList = getField(resultSet, datasourceRequest);
                dataList = data(resultSet, datasourceRequest);
            } finally {
                resetConnectionReadOnly(con, readOnly);
            }
        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
        } catch (RuntimeException e) {
            CrestException.throwException("Datasource connection exception: " + e.getMessage());
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    io.crest.utils.LogUtil.error(e.getMessage(), e);
                }
            }
        }

        map.put("fields", fieldList);
        map.put("data", dataList);
        return map;
    }

    @Override
    public void exec(DatasourceRequest datasourceRequest) throws CrestException {
        DatasourceSchemaDTO value = datasourceRequest.getDsList().entrySet().iterator().next().getValue();
        datasourceRequest.setDatasource(value);
        DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(datasourceRequest.getDatasource().getConfiguration(), value.getType());
        // schema
        ResultSet resultSet = null;
        String oracleCharset = normalizeOracleCharset(datasourceConfiguration.getCharset());
        String oracleTargetCharset = normalizeOracleCharset(datasourceConfiguration.getTargetCharset());

        try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId())) {

            try (Statement statement = getStatement(value, con, datasourceRequest, datasourceConfiguration, null)) {
                if (CollectionUtils.isNotEmpty(datasourceRequest.getTableFieldWithValues())) {
                    LogUtil.info("execWithPreparedStatement uses parameterized SQL");
                    for (int i = 0; i < datasourceRequest.getTableFieldWithValues().size(); i++) {
                        try {
                            Object valueObject = datasourceRequest.getTableFieldWithValues().get(i).getValue();
                            if (valueObject instanceof String && isOracleLike(value.getType())) {
                                if (StringUtils.isNotEmpty(oracleCharset) && StringUtils.isNotEmpty(oracleTargetCharset)) {
                                    //转换为数据库的字符集
                                    valueObject = convertOracleText((String) valueObject, oracleTargetCharset, oracleCharset);
                                }
                                if (datasourceRequest.getTableFieldWithValues().get(i).getType().equals(Types.CLOB)) {
                                    Reader reader = new StringReader((String) valueObject);
                                    ((PreparedStatement) statement).setCharacterStream(i + 1, reader, ((String) valueObject).length());
                                } else {
                                    ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                                }
                            } else {
                                ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                            }
                            LogUtil.info("execWithPreparedStatement param[" + (i + 1) + "](" + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName() + ")");
                        } catch (SQLException e) {
                            throw new SQLException(e.getMessage() + ". TARGET TYPE: " + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName());
                        }
                    }
                    ((PreparedStatement) statement).execute();
                } else {
                    statement.execute(SqlSecurityPolicy.validateSingleStatement(datasourceRequest.getQuery()));
                }
            }

        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
        } catch (RuntimeException e) {
            CrestException.throwException("Datasource connection exception: " + e.getMessage());
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    io.crest.utils.LogUtil.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 针对Oracle特殊处理
     */
    private Statement getStatement(DatasourceSchemaDTO value, Connection con, DatasourceRequest datasourceRequest, DatasourceConfiguration datasourceConfiguration, String autoIncrementPkName) throws SQLException {
        if (isOracleLike(value.getType())) {
            try (Statement sessionStatement = getStatement(con, datasourceConfiguration.getQueryTimeout())) {
                if (StringUtils.isNotBlank(datasourceConfiguration.getSchema())) {
                    sessionStatement.executeUpdate("ALTER SESSION SET CURRENT_SCHEMA = " + quoteOracleSchema(datasourceConfiguration.getSchema()));
                }
                sessionStatement.executeUpdate("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = 'YYYY-MM-DD HH24:MI:SS'");
            }
            //调整字符集
            String oracleCharset = normalizeOracleCharset(datasourceConfiguration.getCharset());
            String oracleTargetCharset = normalizeOracleCharset(datasourceConfiguration.getTargetCharset());
            if (StringUtils.isNotEmpty(oracleCharset) && StringUtils.isNotEmpty(oracleTargetCharset)) {
                datasourceRequest.setQuery(convertOracleText(datasourceRequest.getQuery(), oracleTargetCharset, oracleCharset));
            }
        }
        return getPreparedStatement(con, datasourceConfiguration.getQueryTimeout(), datasourceRequest.getQuery(), datasourceRequest.getTableFieldWithValues(), autoIncrementPkName, datasourceConfiguration);
    }

    @Override
    public ExecuteResult executeUpdate(DatasourceRequest datasourceRequest, String autoIncrementPkName) throws CrestException {
        DatasourceSchemaDTO value = datasourceRequest.getDsList().entrySet().iterator().next().getValue();
        datasourceRequest.setDatasource(value);
        DatasourceConfiguration datasourceConfiguration = parseDatasourceConfiguration(datasourceRequest.getDatasource().getConfiguration(), value.getType());
        // schema
        ResultSet resultSet = null;
        String oracleCharset = normalizeOracleCharset(datasourceConfiguration.getCharset());
        String oracleTargetCharset = normalizeOracleCharset(datasourceConfiguration.getTargetCharset());
        try (Connection con = getConnectionFromPool(datasourceRequest.getDatasource().getId())) {

            try (Statement statement = getStatement(value, con, datasourceRequest, datasourceConfiguration, autoIncrementPkName)) {
                int count = 0;
                if (CollectionUtils.isNotEmpty(datasourceRequest.getTableFieldWithValues())) {
                    LogUtil.info("execWithPreparedStatement uses parameterized SQL");
                    for (int i = 0; i < datasourceRequest.getTableFieldWithValues().size(); i++) {
                        try {
                            Object valueObject = datasourceRequest.getTableFieldWithValues().get(i).getValue();

                            if (valueObject instanceof String && isOracleLike(value.getType())) {
                                if (StringUtils.isNotEmpty(oracleCharset) && StringUtils.isNotEmpty(oracleTargetCharset)) {
                                    //转换为数据库的字符集
                                    valueObject = convertOracleText((String) valueObject, oracleTargetCharset, oracleCharset);
                                }
                                if (datasourceRequest.getTableFieldWithValues().get(i).getType().equals(Types.CLOB)) {
                                    Reader reader = new StringReader((String) valueObject);
                                    ((PreparedStatement) statement).setCharacterStream(i + 1, reader, ((String) valueObject).length());
                                } else {
                                    ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                                }
                            } else {
                                ((PreparedStatement) statement).setObject(i + 1, valueObject, datasourceRequest.getTableFieldWithValues().get(i).getType());
                            }
                            LogUtil.info("execWithPreparedStatement param[" + (i + 1) + "](" + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName() + ")");
                        } catch (SQLException e) {
                            throw new SQLException(e.getMessage() + ". TARGET TYPE: " + datasourceRequest.getTableFieldWithValues().get(i).getColumnTypeName());
                        }
                    }
                    count = ((PreparedStatement) statement).executeUpdate();
                } else {
                    count = statement.executeUpdate(SqlSecurityPolicy.validateSingleStatement(datasourceRequest.getQuery()));
                }

                ExecuteResult result = new ExecuteResult();
                result.setCount(count);

                if (StringUtils.isNotBlank(autoIncrementPkName)) {
                    List<String> generatedKeys = new ArrayList<>();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        while (keys.next()) {
                            generatedKeys.add(keys.getObject(1).toString());
                        }
                    }
                    result.setGeneratedKeys(generatedKeys);
                }

                return result;
            }
        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
        } catch (RuntimeException e) {
            CrestException.throwException("Datasource connection exception: " + e.getMessage());
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    io.crest.utils.LogUtil.error(e.getMessage(), e);
                }
            }
        }

        return new ExecuteResult();
    }

    private List<TableField> getField(ResultSet rs, DatasourceRequest datasourceRequest) throws SQLException {
        List<TableField> fieldList = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int j = 0; j < columnCount; j++) {
            String f = metaData.getColumnName(j + 1);
            if (Strings.CI.contains(f, "ROWNUM")) {
                continue;
            }
            String l = StringUtils.isNotEmpty(metaData.getColumnLabel(j + 1)) ? metaData.getColumnLabel(j + 1) : f;
            String t = metaData.getColumnTypeName(j + 1).toUpperCase();
            TableField field = new TableField();
            field.setOriginName(l);
            field.setName(l);
            field.setNativeType(t);
            field.setType(t);
            fieldList.add(field);
        }
        return fieldList;
    }

    private List<String[]> data(ResultSet rs, DatasourceRequest datasourceRequest) throws SQLException {
        String targetCharset = null;
        String originCharset = null;
        if (datasourceRequest != null && isOracleLike(datasourceRequest.getDatasource().getType())) {
            DatasourceConfiguration jdbcConfiguration = parseDatasourceConfiguration(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getDatasource().getType());

            if (StringUtils.isNotEmpty(jdbcConfiguration.getCharset())) {
                originCharset = normalizeOracleCharset(jdbcConfiguration.getCharset());
            }
            if (StringUtils.isNotEmpty(jdbcConfiguration.getTargetCharset())) {
                targetCharset = normalizeOracleCharset(jdbcConfiguration.getTargetCharset());
            }
        }
        List<String[]> list = new LinkedList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int j = 0; j < columnCount; j++) {
                int columnType = metaData.getColumnType(j + 1);
                switch (columnType) {
                    case Types.DATE:
                        if (rs.getDate(j + 1) != null) {
                            row[j] = rs.getDate(j + 1).toString();
                        }
                        break;
                    case Types.TIMESTAMP:
                        if (rs.getTimestamp(j + 1) != null) {
                            row[j] = rs.getTimestamp(j + 1).toString();
                        }
                        break;
                    case Types.BOOLEAN:
                        row[j] = rs.getBoolean(j + 1) ? "1" : "0";
                        break;
                    case Types.NUMERIC:
                        BigDecimal bigDecimal = rs.getBigDecimal(j + 1);
                        row[j] = bigDecimal == null ? null : bigDecimal.toString();
                        break;
                    default:
                        if (metaData.getColumnTypeName(j + 1).toLowerCase().equalsIgnoreCase("blob")) {
                            row[j] = rs.getBlob(j + 1) == null ? "" : rs.getBlob(j + 1).toString();
                        }
                        if (targetCharset != null && StringUtils.isNotEmpty(rs.getString(j + 1)) && columnType == Types.CLOB) {
                            Charset target = Charset.forName(targetCharset);
                            if (originCharset == null) {
                                row[j] = new String(rs.getString(j + 1).getBytes(), target);
                            } else {
                                row[j] = new String(rs.getString(j + 1).getBytes(Charset.forName(originCharset)), target);
                            }
                        } else if (targetCharset != null && StringUtils.isNotEmpty(rs.getString(j + 1)) && (columnType != Types.NVARCHAR && columnType != Types.NCHAR)) {
                            row[j] = new String(rs.getBytes(j + 1), Charset.forName(targetCharset));
                        } else {
                            row[j] = rs.getString(j + 1);
                        }

                        break;
                }
            }
            list.add(row);
        }
        return list;
    }

    private String normalizeOracleCharset(String charset) {
        if (StringUtils.isBlank(charset) || Strings.CI.equals(charset, "Default")) {
            return null;
        }
        String normalized = Strings.CI.equals(charset, "US7ASCII") ? "US-ASCII" : charset;
        try {
            return Charset.forName(normalized).name();
        } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
            CrestException.throwException("Unsupported charset: " + charset);
        }
        return null;
    }

    private String convertOracleText(String value, String fromCharset, String toCharset) {
        if (StringUtils.isEmpty(value) || StringUtils.isBlank(fromCharset) || StringUtils.isBlank(toCharset)) {
            return value;
        }
        try {
            return new String(value.getBytes(fromCharset), toCharset);
        } catch (UnsupportedEncodingException e) {
            CrestException.throwException("Unsupported charset: " + e.getMessage());
            return value;
        }
    }

    @Override
    public void hidePW(DatasourceDTO datasourceDTO) {
        DatasourceConfiguration configuration = null;
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(datasourceDTO.getType());
        switch (datasourceType) {
            case mysql:
            case mongo:
            case mariadb:
            case TiDB:
            case StarRocks:
            case doris:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), Mysql.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split("\\?")[1].split("&");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            case obOracle:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), ObOracle.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split("\\?")[1].split("&");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            case pg:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), Pg.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split("\\?")[1].split("&");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            case redshift:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), Redshift.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split("\\?")[1].split("&");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            case ck:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), CK.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split("\\?")[1].split("&");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            case impala:
                configuration = JsonUtil.parseObject(datasourceDTO.getConfiguration(), Impala.class);
                if (StringUtils.isNotEmpty(configuration.getUrlType()) && configuration.getUrlType().equalsIgnoreCase("jdbcUrl")) {
                    if (configuration.getJdbcUrl().contains("password=")) {
                        String[] params = configuration.getJdbcUrl().split(";");
                        String pd = "";
                        for (int i = 0; i < params.length; i++) {
                            if (params[i].contains("password=")) {
                                pd = params[i];
                            }
                        }
                        configuration.setJdbcUrl(configuration.getJdbcUrl().replace(pd, "password=******"));
                        datasourceDTO.setConfiguration(JsonUtil.toJSONString(configuration).toString());
                    }
                }
                break;
            default:
                break;
        }
    }

    static void applyOracleColumnComments(List<TableField> fields, Map<String, String> columnComments) {
        if (CollectionUtils.isEmpty(fields) || columnComments == null || columnComments.isEmpty()) {
            return;
        }
        for (TableField field : fields) {
            String comments = columnComments.get(normalizeOracleName(field.getOriginName()));
            if (StringUtils.isNotBlank(comments)) {
                field.setName(comments.trim());
            }
        }
    }

    private Map<String, String> getOracleColumnComments(Connection connection, String schema, String table) throws SQLException {
        Map<String, String> comments = new HashMap<>();
        if (StringUtils.isBlank(schema) || StringUtils.isBlank(table)) {
            return comments;
        }
        String sql = "select column_name, comments from all_col_comments where owner = ? and table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeOracleName(schema));
            statement.setString(2, normalizeOracleName(table));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    comments.put(normalizeOracleName(resultSet.getString("column_name")), resultSet.getString("comments"));
                }
            }
        }
        return comments;
    }

    private static String normalizeOracleName(String name) {
        return StringUtils.trimToEmpty(name).toUpperCase(Locale.ROOT);
    }

    private TableField getTableFieldDesc(DatasourceRequest datasourceRequest, ResultSet resultSet, int commentIndex, Map<String, Integer> tableTypeMap) throws SQLException {
        TableField tableField = new TableField();
        tableField.setOriginName(resultSet.getString(1));
        tableField.setType(resultSet.getString(2).toUpperCase());
        tableField.setNativeType(tableField.getType());
        int fieldType = FieldUtils.resolveFieldType(tableField.getType());
        tableField.setExtractedFieldType(fieldType);
        tableField.setFieldType(fieldType);
        tableField.setName(resultSet.getString(commentIndex));
        try {
            tableField.setPrimary(resultSet.getInt(4) > 0);
        } catch (SQLException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
        try {
            if (StringUtils.endsWithIgnoreCase(datasourceRequest.getDatasource().getType(), "oracle")) {
                if (Strings.CS.contains(resultSet.getString(5), "nextval") || Strings.CI.equals(resultSet.getString(5), "GENERATED ALWAYS AS IDENTITY")) {
                    tableField.setAutoIncrement(true);
                }
            } else {
                tableField.setAutoIncrement(resultSet.getInt(5) > 0);
            }
        } catch (SQLException | RuntimeException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
        try {
            tableField.setTypeNumber(tableTypeMap.get(StringUtils.lowerCase(tableField.getOriginName())));
        } catch (RuntimeException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
        return tableField;
    }

    public Connection initConnection(Map<Long, DatasourceSchemaDTO> dsMap) throws SQLException {
        Connection connection = take();
        CalciteConnection calciteConnection = null;
        calciteConnection = connection.unwrap(CalciteConnection.class);
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        datasourceRequest.setDsList(dsMap);
        buildSchema(datasourceRequest, calciteConnection);
        return connection;
    }

    private void registerDriver() {
        for (String driverClass : getDriver()) {
            try {
                Driver driver = (Driver) extendedJdbcClassLoader.loadClass(driverClass).newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException | RuntimeException e) {
                LogUtil.debug("Skip unavailable datasource driver: " + driverClass);
            }
        }
    }

    private Connection getCalciteConnection() {
        registerDriver();
        Properties info = new Properties();
        info.setProperty(CalciteConnectionProperty.LEX.camelName(), "JAVA");
        info.setProperty(CalciteConnectionProperty.FUN.camelName(), "all");
        info.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
        info.setProperty(CalciteConnectionProperty.PARSER_FACTORY.camelName(), "org.apache.calcite.sql.parser.impl.SqlParserImpl#FACTORY");
        info.setProperty(CalciteConnectionProperty.DEFAULT_NULL_COLLATION.camelName(), NullCollation.LAST.name());
        info.setProperty("remarks", "true");
        Connection connection = null;
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:calcite:", info);
        } catch (ClassNotFoundException | SQLException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        return connection;
    }

    private void applyDatasourceUrl(BasicDataSource dataSource, DatasourceSchemaDTO ds, DatasourceConfiguration configuration) {
        CoreDriver customDriver = resolveCustomDriver(ds.getType(), configuration.getCustomDriver());
        String driverClassName = JdbcUrlSecurityPolicy.resolveDriverClass(ds.getType(), configuration.getDriver(), configuration.getCustomDriver(), customDriver);
        configuration.setDriver(driverClassName);
        dataSource.setDriverClassName(driverClassName);
        if (!JdbcUrlSecurityPolicy.isDefaultCustomDriver(configuration.getCustomDriver())) {
            dataSource.setDriverClassLoader(getCustomJdbcClassLoader(customDriver));
        }
        dataSource.setUrl(JdbcUrlSecurityPolicy.validate(ds.getType(), configuration.getJdbc(), configuration.getExtraParams()));
    }

    // 构建root schema
    private synchronized SchemaPlus buildSchema(DatasourceRequest datasourceRequest, CalciteConnection calciteConnection) {
        SchemaPlus rootSchema = calciteConnection.getRootSchema();
        Map<Long, DatasourceSchemaDTO> dsList = datasourceRequest.getDsList();
        for (Map.Entry<Long, DatasourceSchemaDTO> next : dsList.entrySet()) {
            DatasourceSchemaDTO ds = next.getValue();
            BasicDataSource dataSource = null;
            try {
                dataSource = new BasicDataSource();
                dataSource.setMaxWaitMillis(5 * 1000);
                dataSource.setTestWhileIdle(true);
                dataSource.setTestOnBorrow(true);
                dataSource.setTestOnReturn(true);
                dataSource.setTimeBetweenEvictionRunsMillis(60 * 1000);
                dataSource.setValidationQuery("select 1");
                dataSource.setValidationQueryTimeout(5);
                Schema schema = null;
                DatasourceConfiguration configuration = null;
                DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(ds.getType());
                        if (rootSchema.getSubSchema(ds.getSchemaAlias()) != null) {
                            JdbcSchema jdbcSchema = rootSchema.getSubSchema(ds.getSchemaAlias()).unwrap(JdbcSchema.class);
                            BasicDataSource basicDataSource = (BasicDataSource) jdbcSchema.getDataSource();
                            basicDataSource.close();
                            removeSubSchema(rootSchema, ds.getSchemaAlias());
                        }
                        switch (datasourceType) {
                            case mysql:
                            case mongo:
                            case mariadb:
                            case TiDB:
                            case StarRocks:
                            case doris:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Mysql.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getDataBase());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case impala:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Impala.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getDataBase());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case sqlServer:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Sqlserver.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getSchema());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case oracle:
                            case obOracle:
                                dataSource.setValidationQuery("SELECT 1 FROM DUAL");
                                configuration = parseOracleLikeConfiguration(ds.getConfiguration(), ds.getType());
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getSchema());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case db2:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Db2.class);
                                dataSource.setValidationQuery("select 1 from syscat.tables  WHERE TABSCHEMA ='CREST_SCHEMA' AND \"TYPE\" = 'T'".replace("CREST_SCHEMA", configuration.getSchema()));
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getSchema());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case ck:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), CK.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getDataBase());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case pg:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Pg.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getSchema());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case redshift:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Redshift.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getSchema());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            case h2:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), H2.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getDataBase());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                                break;
                            default:
                                configuration = parseDatasourceConfiguration(ds.getConfiguration(), Mysql.class);
                                if (StringUtils.isNotBlank(configuration.getUsername())) {
                                    dataSource.setUsername(configuration.getUsername());
                                }
                                if (StringUtils.isNotBlank(configuration.getPassword())) {
                                    dataSource.setPassword(configuration.getPassword());
                                }
                                dataSource.setInitialSize(configuration.getInitialPoolSize());
                                dataSource.setMaxTotal(configuration.getMaxPoolSize());
                                dataSource.setMinIdle(configuration.getMinPoolSize());
                                dataSource.setDefaultQueryTimeout(Integer.valueOf(configuration.getQueryTimeout()));
                                startSshSession(configuration, null, ds.getId());
                                applyDatasourceUrl(dataSource, ds, configuration);
                                schema = JdbcSchema.create(rootSchema, ds.getSchemaAlias(), dataSource, null, configuration.getDataBase());
                                rootSchema.add(ds.getSchemaAlias(), schema);
                        }
            } catch (CrestException e) {
                closeDataSourceIfUnregistered(rootSchema, ds.getSchemaAlias(), dataSource);
                LogUtil.debug("Skip unavailable datasource pool: " + ds.getName() + ", " + e.getMessage());
            } catch (SQLException | RuntimeException e) {
                closeDataSourceIfUnregistered(rootSchema, ds.getSchemaAlias(), dataSource);
                LogUtil.error("Skip unavailable datasource pool: " + ds.getName() + ", " + e.getMessage(), e);
            }
        }
        return rootSchema;
    }

    private void closeDataSourceIfUnregistered(SchemaPlus rootSchema, String schemaAlias, BasicDataSource dataSource) {
        if (dataSource == null || rootSchema.getSubSchema(schemaAlias) != null) {
            return;
        }
        try {
            dataSource.close();
        } catch (SQLException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    private List<String[]> dataResult(ResultSet rs) {
        List<String[]> list = new LinkedList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int j = 0; j < columnCount; j++) {
                    int columnType = metaData.getColumnType(j + 1);
                    switch (columnType) {
                        case Types.DATE:
                            if (rs.getDate(j + 1) != null) {
                                row[j] = rs.getDate(j + 1).toString();
                            }
                            break;
                        case Types.BOOLEAN:
                            row[j] = rs.getBoolean(j + 1) ? "true" : "false";
                            break;
                        default:
                            if (metaData.getColumnTypeName(j + 1).toLowerCase().equalsIgnoreCase("blob")) {
                                row[j] = rs.getBlob(j + 1) == null ? "" : rs.getBlob(j + 1).toString();
                            } else {
                                row[j] = rs.getString(j + 1);
                            }
                            break;
                    }
                }
                list.add(row);
            }
        } catch (SQLException e) {
            CrestException.throwException(e.getMessage());
        }
        return list;
    }

    private String getTableFiledSql(DatasourceRequest datasourceRequest) {
        String sql = "";
        DatasourceConfiguration configuration = null;
        String database = "";
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(datasourceRequest.getDatasource().getType());
        switch (datasourceType) {
            case StarRocks:
            case doris:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Mysql.class);
                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                if (database.contains(".")) {
                    sql = "select * from " + quoteQualifiedIdentifier(datasourceRequest.getTable()) + " limit 0 offset 0 ";
                } else {
                    sql = String.format("SELECT COLUMN_NAME,DATA_TYPE,COLUMN_COMMENT,IF(COLUMN_KEY='PRI',1,0),IF(EXTRA LIKE '%%auto_increment%%',1,0) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s", sqlLiteral(database), sqlLiteral(datasourceRequest.getTable()));
                }
                break;
            case mysql:
            case mongo:
            case mariadb:
            case TiDB:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Mysql.class);
                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                sql = String.format("SELECT COLUMN_NAME,DATA_TYPE,COLUMN_COMMENT,IF(COLUMN_KEY='PRI',1,0),IF(EXTRA LIKE '%%auto_increment%%',1,0) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s", sqlLiteral(database), sqlLiteral(datasourceRequest.getTable()));
                break;
            case oracle:
            case obOracle:
                configuration = parseOracleLikeConfiguration(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getDatasource().getType());
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                sql = String.format("""
                        SELECT tc.COLUMN_NAME AS ColumnName,
                               tc.DATA_TYPE,
                               cc.COMMENTS,
                               CASE
                                   WHEN ac.COLUMN_NAME IS NOT NULL THEN 1
                                   ELSE 0
                                   END,
                               tc.DATA_DEFAULT
                        FROM ALL_TAB_COLUMNS tc
                                 LEFT JOIN (SELECT cols.OWNER,
                                                   cols.TABLE_NAME,
                                                   cols.COLUMN_NAME
                                            FROM ALL_CONSTRAINTS cons
                                                     JOIN
                                                 ALL_CONS_COLUMNS cols
                                                 ON cons.OWNER = cols.OWNER
                                                     AND cons.CONSTRAINT_NAME = cols.CONSTRAINT_NAME
                                            WHERE cons.TABLE_NAME = %s
                                              AND cons.CONSTRAINT_TYPE = 'P') ac
                                           ON tc.OWNER = ac.OWNER
                                               AND tc.TABLE_NAME = ac.TABLE_NAME
                                               AND tc.COLUMN_NAME = ac.COLUMN_NAME
                                 LEFT JOIN ALL_COL_COMMENTS cc
                                           ON tc.owner = cc.owner AND tc.table_name = cc.table_name AND tc.column_name = cc.column_name
                        WHERE tc.TABLE_NAME = %s
                          AND tc.OWNER = %s
                        ORDER BY tc.TABLE_NAME, tc.COLUMN_ID
                        """, sqlLiteral(datasourceRequest.getTable()), sqlLiteral(datasourceRequest.getTable()), sqlLiteral(configuration.getSchema()));
                break;
            case db2:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Db2.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                sql = String.format("SELECT COLNAME, TYPENAME, REMARKS, 0, 0 FROM SYSCAT.COLUMNS WHERE TABSCHEMA = %s AND TABNAME = %s ", sqlLiteral(configuration.getSchema()), sqlLiteral(datasourceRequest.getTable()));
                break;
            case sqlServer:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Sqlserver.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }

                sql = String.format("""
                        SELECT
                            c.name,
                            t.name,
                            CAST(ep.value AS NVARCHAR(4000)),
                            CASE
                                WHEN pk.column_id IS NOT NULL THEN 1
                                ELSE 0
                                END,
                            COLUMNPROPERTY(c.object_id, c.name, 'IsIdentity')
                        FROM sys.columns AS c
                                 INNER JOIN sys.objects AS o ON c.object_id = o.object_id
                                 INNER JOIN sys.schemas AS s ON o.schema_id = s.schema_id
                                 LEFT JOIN sys.types AS t ON c.user_type_id = t.user_type_id
                                 LEFT JOIN sys.extended_properties AS ep
                                           ON c.object_id = ep.major_id
                                               AND c.column_id = ep.minor_id
                                               AND ep.name = 'MS_Description'
                                 LEFT JOIN (
                            SELECT ic.object_id, ic.column_id
                            FROM sys.indexes i
                                     INNER JOIN sys.index_columns ic
                                                ON i.object_id = ic.object_id
                                                    AND i.index_id = ic.index_id
                            WHERE i.is_primary_key = 1
                        ) pk ON c.object_id = pk.object_id AND c.column_id = pk.column_id
                        WHERE o.name = %s
                          AND s.name = %s
                        ORDER BY c.column_id
                        """, sqlLiteral(datasourceRequest.getTable()), sqlLiteral(configuration.getSchema()));
                break;
            case pg:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Pg.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                sql = String.format("""
                        SELECT a.attname     AS ColumnName,
                               t.typname,
                               b.description AS ColumnDescription,
                               CASE
                                   WHEN d.indisprimary THEN 1
                                   ELSE 0
                                   END,
                               CASE
                                   WHEN pg_get_expr(ad.adbin, ad.adrelid) LIKE 'nextval%%' THEN 1
                        """ + (datasourceRequest.getDsVersion() > 9 ? """
                                   WHEN a.attidentity = 'd' THEN 1
                                   WHEN a.attidentity = 'a' THEN 1
                        """ : "") + """
                                   ELSE 0
                                   END
                        FROM pg_class c
                                 JOIN pg_attribute a ON a.attrelid = c.oid
                                 LEFT JOIN pg_attrdef ad ON a.attrelid = ad.adrelid AND a.attnum = ad.adnum
                                 LEFT JOIN pg_description b ON a.attrelid = b.objoid AND a.attnum = b.objsubid
                                 JOIN pg_type t ON a.atttypid = t.oid
                                 LEFT JOIN pg_index d ON d.indrelid = a.attrelid AND d.indisprimary AND a.attnum = ANY (d.indkey)
                        where c.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = %s)
                          AND c.relname = %s
                          AND a.attnum > 0
                          AND NOT a.attisdropped
                        ORDER BY a.attnum;
                        """, sqlLiteral(configuration.getSchema()), sqlLiteral(datasourceRequest.getTable()));
                break;
            case redshift:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), CK.class);
                sql = String.format("SELECT\n" + "    a.attname AS ColumnName,\n" + "    t.typname,\n" + "    b.description AS ColumnDescription,\n" + "    0, 0\n" + "FROM\n" + "    pg_class c\n" + "    JOIN pg_attribute a ON a.attrelid = c.oid\n" + "    LEFT JOIN pg_description b ON a.attrelid = b.objoid AND a.attnum = b.objsubid\n" + "    JOIN pg_type t ON a.atttypid = t.oid\n" + "WHERE\n" + "    c.relname = %s\n" + "    AND a.attnum > 0\n" + "    AND NOT a.attisdropped\n" + "ORDER BY\n" + "    a.attnum\n" + "   ", sqlLiteral(datasourceRequest.getTable()));
                break;
            case ck:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), CK.class);

                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                sql = String.format(" SELECT\n" + "    name,\n" + "    type,\n" + "    comment,\n" + "    0, 0\n" + "FROM\n" + "    system.columns\n" + "WHERE\n" + "    database = %s  \n" + "    AND table = %s ", sqlLiteral(database), sqlLiteral(datasourceRequest.getTable()));
                break;
            case impala:
                sql = String.format("DESCRIBE %s", quoteQualifiedIdentifier(datasourceRequest.getTable()));
                break;
            case h2:
                sql = String.format("SELECT COLUMN_NAME, DATA_TYPE, REMARKS, 0, 0 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = %s", sqlLiteral(datasourceRequest.getTable()));
                break;
            default:
                break;
        }

        return sql;
    }

    private List<QueryAndParams> tablesSql(DatasourceRequest datasourceRequest) throws CrestException {
        List<QueryAndParams> tableSqls = new ArrayList<>();
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(datasourceRequest.getDatasource().getType());
        DatasourceConfiguration configuration = null;
        String database = "";
        switch (datasourceType) {
            case StarRocks:
            case doris:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Mysql.class);
                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                if (database.contains(".")) {
                    tableSqls.add(new QueryAndParams("show tables"));
                } else {
                    tableSqls.add(new QueryAndParams("SELECT TABLE_NAME,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?", database));
                }
                break;
            case mongo:
                tableSqls.add(new QueryAndParams("show tables"));
                break;
            case mysql:
            case mariadb:
            case TiDB:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Mysql.class);
                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                tableSqls.add(new QueryAndParams("SELECT TABLE_NAME,TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?", database));
                break;
            case oracle:
            case obOracle:
                configuration = parseOracleLikeConfiguration(datasourceRequest.getDatasource().getConfiguration(), datasourceRequest.getDatasource().getType());
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                tableSqls.add(new QueryAndParams("select table_name, comments, owner from all_tab_comments where owner = ? AND table_type = 'TABLE'", configuration.getSchema()));
                tableSqls.add(new QueryAndParams("select table_name, comments, owner from all_tab_comments where owner = ? AND table_type = 'VIEW'", configuration.getSchema()));
                if (!isObOracle(datasourceRequest.getDatasource().getType())) {
                    tableSqls.add(new QueryAndParams("SELECT \n" + "    m.mview_name,\n" + "    c.comments\n" + "FROM \n" + "    ALL_MVIEWS m\n" + "LEFT JOIN \n" + "    ALL_TAB_COMMENTS c \n" + "ON \n" + "    m.owner = c.owner \n" + "    AND m.mview_name = c.table_name\n" + "    AND c.table_type = 'MATERIALIZED VIEW'\n" + "WHERE m.OWNER = ?", configuration.getSchema()));
                }
                break;
            case db2:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Db2.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                tableSqls.add(new QueryAndParams("select TABNAME, REMARKS from syscat.tables WHERE TABSCHEMA = ? AND \"TYPE\" = 'T'", configuration.getSchema()));
                break;
            case sqlServer:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Sqlserver.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                tableSqls.add(new QueryAndParams("SELECT   \n" + "    t.name AS TableName,  \n" + "    ep.value AS TableDescription  \n" + "FROM   \n" + "    sys.tables t  \n" + "LEFT OUTER JOIN   sys.schemas sc ON sc.schema_id =t.schema_id \n" + "LEFT OUTER JOIN   \n" + "    sys.extended_properties ep ON t.object_id = ep.major_id   \n" + "                               AND ep.minor_id = 0   \n" + "                               AND ep.class = 1  \n" + "                               AND ep.name = 'MS_Description'\n" + "where sc.name = ?", configuration.getSchema()));
                tableSqls.add(new QueryAndParams("SELECT   \n" + "    t.name AS TableName,  \n" + "    ep.value AS TableDescription  \n" + "FROM   \n" + "    sys.views t  \n" + "LEFT OUTER JOIN   sys.schemas sc ON sc.schema_id =t.schema_id \n" + "LEFT OUTER JOIN   \n" + "    sys.extended_properties ep ON t.object_id = ep.major_id   \n" + "                               AND ep.minor_id = 0   \n" + "                               AND ep.class = 1  \n" + "                               AND ep.name = 'MS_Description'\n" + "where sc.name = ?", configuration.getSchema()));
                break;
            case pg:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), Pg.class);
                if (StringUtils.isEmpty(configuration.getSchema())) {
                    CrestException.throwException(Translator.get("i18n_schema_is_empty"));
                }
                tableSqls.add(new QueryAndParams("SELECT  \n" + "    relname AS TableName,  \n" + "    obj_description(relfilenode::regclass, 'pg_class') AS TableDescription  \n" + "FROM  \n" + "    pg_class  \n" + "WHERE  \n" + "   relkind in  ('r','p', 'f')  \n" + "    AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?) ", configuration.getSchema()));
                tableSqls.add(new QueryAndParams("SELECT \n" + "    c.relname AS view_name,\n" + "    COALESCE(d.description, 'No description provided') AS view_description\n" + "FROM \n" + "    pg_class c\n" + "JOIN \n" + "    pg_namespace n ON c.relnamespace = n.oid\n" + "LEFT JOIN \n" + "    pg_description d ON c.oid = d.objoid\n" + "WHERE \n" + "    c.relkind = 'v'  \n" + "    AND n.nspname = ?", configuration.getSchema()));
                tableSqls.add(new QueryAndParams("SELECT \n" + "    c.relname AS materialized_view_name,\n" + "    COALESCE(d.description, '') AS view_description\n" + "FROM \n" + "    pg_class c\n" + "JOIN \n" + "    pg_namespace n ON c.relnamespace = n.oid\n" + "LEFT JOIN \n" + "    pg_description d ON c.oid = d.objoid\n" + "WHERE \n" + "    c.relkind = 'm' and n.nspname = ?", configuration.getSchema()));
                break;
            case redshift:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), CK.class);
                tableSqls.add(new QueryAndParams("SELECT  \n" + "    relname AS TableName,  \n" + "    obj_description(relfilenode::regclass, 'pg_class') AS TableDescription  \n" + "FROM  \n" + "    pg_class  \n" + "WHERE  \n" + "   relkind in  ('r','p', 'f')  \n" + "    AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = ?) ", configuration.getSchema()));
                break;
            case ck:
                configuration = JsonUtil.parseObject(datasourceRequest.getDatasource().getConfiguration(), CK.class);
                if (StringUtils.isEmpty(configuration.getUrlType()) || configuration.getUrlType().equalsIgnoreCase("hostName")) {
                    database = configuration.getDataBase();
                } else {
                    database = databaseFromJdbcUrl(configuration.getJdbcUrl());
                }
                if (datasourceRequest.getDsVersion() < 22) {
                    tableSqls.add(new QueryAndParams("SELECT name, name FROM system.tables where database = ?", database));
                } else {
                    tableSqls.add(new QueryAndParams("SELECT name, comment FROM system.tables where database = ?", database));
                }


                break;
            default:
                tableSqls.add(new QueryAndParams("show tables"));
        }
        return tableSqls;

    }

    private void configureStatement(PreparedStatement statement, QueryAndParams queryAndParams, int queryTimeout) throws SQLException {
        statement.setQueryTimeout(queryTimeout);
        List<Object> params = queryAndParams.params();
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private record QueryAndParams(String sql, List<Object> params) {
        private QueryAndParams(String sql) {
            this(sql, Collections.emptyList());
        }

        private QueryAndParams(String sql, Object... params) {
            this(sql, Arrays.asList(params));
        }
    }

    private String getSchemaSql(DatasourceDTO datasource) throws CrestException {
        DatasourceConfiguration.DatasourceType datasourceType = DatasourceConfiguration.DatasourceType.valueOf(datasource.getType());
        switch (datasourceType) {
            case oracle:
            case obOracle:
                return "select * from all_users";
            case sqlServer:
                return "select name from sys.schemas;";
            case db2:
                DatasourceConfiguration configuration = JsonUtil.parseObject(datasource.getConfiguration(), Db2.class);
                return "select SCHEMANAME from syscat.SCHEMATA WHERE \"DEFINER\" = "
                        + sqlLiteral(configuration.getUsername().toUpperCase(Locale.ROOT));
            case pg:
                return "SELECT nspname FROM pg_namespace;";
            case redshift:
                return "SELECT nspname FROM pg_namespace;";
            default:
                return "show tables;";
        }
    }

    public Statement getStatement(Connection connection, int queryTimeout) {
        if (connection == null) {
            CrestException.throwException("Failed to get connection!");
        }
        Statement stat = null;
        try {
            stat = connection.createStatement();
            stat.setQueryTimeout(queryTimeout);
        } catch (SQLException e) {
            CrestException.throwException(e.getMessage());
        }
        return stat;
    }

    public Statement getPreparedStatement(Connection connection, int queryTimeout, String sql, List<TableFieldWithValue> values) {
        return getPreparedStatement(connection, queryTimeout, sql, values, null, null);
    }

    @SuppressWarnings("java/sql-injection")
    public Statement getPreparedStatement(Connection connection, int queryTimeout, String sql, List<TableFieldWithValue> values, String autoIncrementPkName, DatasourceConfiguration datasourceConfiguration) {
        if (connection == null) {
            CrestException.throwException("Failed to get connection!");
        }
        if (CollectionUtils.isNotEmpty(values)) {
            PreparedStatement stat = null;
            String pkName = autoIncrementPkName;
            try {
                if (StringUtils.isNotBlank(autoIncrementPkName)) {
                    String[] generatedColumns = {pkName};
                    String validatedSql = SqlSecurityPolicy.validateSingleStatement(sql);
                    stat = connection.prepareStatement(validatedSql, generatedColumns);
                } else {
                    String validatedSql = SqlSecurityPolicy.validateSingleStatement(sql);
                    stat = connection.prepareStatement(validatedSql);
                }
                stat.setQueryTimeout(queryTimeout);
            } catch (SQLException | RuntimeException e) {
                CrestException.throwException(e.getMessage());
            }
            return stat;
        } else {
            return getStatement(connection, queryTimeout);
        }
    }

    protected boolean isDefaultClassLoader(String customDriver) {
        return StringUtils.isEmpty(customDriver) || customDriver.equalsIgnoreCase("default");
    }

    protected ExtendedJdbcClassLoader getCustomJdbcClassLoader(CoreDriver coreDriver) {
        if (coreDriver == null) {
            CrestException.throwException("Can not found custom Driver");
        }
        ExtendedJdbcClassLoader customJdbcClassLoader = customJdbcClassLoaders.get(coreDriver.getId());
        if (customJdbcClassLoader == null) {
            return addCustomJdbcClassLoader(coreDriver);
        } else {
            if (StringUtils.isNotEmpty(customJdbcClassLoader.getDriver()) && customJdbcClassLoader.getDriver().equalsIgnoreCase(coreDriver.getDriverClass())) {
                return customJdbcClassLoader;
            } else {
                customJdbcClassLoaders.remove(coreDriver.getId());
                return addCustomJdbcClassLoader(coreDriver);
            }
        }
    }

    private synchronized ExtendedJdbcClassLoader addCustomJdbcClassLoader(CoreDriver coreDriver) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader.getParent() != null) {
            classLoader = classLoader.getParent();
            if (classLoader.toString().contains("ExtClassLoader")) {
                break;
            }
        }
        try {
            File file = FileUtils.ensureSubDirectory(CUSTOM_PATH, String.valueOf(coreDriver.getId()));
            ExtendedJdbcClassLoader customJdbcClassLoader = new ExtendedJdbcClassLoader(new URL[]{file.toURI().toURL()}, classLoader);
            customJdbcClassLoader.setDriver(coreDriver.getDriverClass());
            File[] array = file.listFiles();
            Optional.ofNullable(array).ifPresent(files -> {
                for (File tmp : array) {
                    if (tmp.getName().endsWith(".jar")) {
                        try {
                            customJdbcClassLoader.addFile(tmp);
                        } catch (IOException e) {
                            io.crest.utils.LogUtil.error(e.getMessage(), e);
                        }
                    }
                }
            });
            customJdbcClassLoaders.put(coreDriver.getId(), customJdbcClassLoader);
            return customJdbcClassLoader;
        } catch (IOException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        return null;
    }

    private final AtomicReference<Connection> calciteConnectionRef = new AtomicReference<>();

    public void initConnectionPool() {
        LogUtil.info("Begin to init datasource pool...");
        QueryWrapper<CoreDatasource> datasourceQueryWrapper = new QueryWrapper();
        List<CoreDatasource> coreDatasources = coreDatasourceMapper.selectList(datasourceQueryWrapper).stream()
                .filter(coreDatasource -> !Arrays.asList("folder", "API", "Excel", "ExcelRemote").contains(coreDatasource.getType()))
                .filter(coreDatasource -> Strings.CI.equals(coreDatasource.getStatus(), "Success"))
                .collect(Collectors.toList());
        CoreDatasource engine = engineManage.engineDatasource();
        if (engine != null) {
            coreDatasources.add(engine);
        }

        for (CoreDatasource coreDatasource : coreDatasources) {
            Map<Long, DatasourceSchemaDTO> dsMap = new HashMap<>();
            DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
            BeanUtils.copyBean(datasourceSchemaDTO, coreDatasource);
            datasourceSchemaDTO.setSchemaAlias(String.format(SQLConstants.SCHEMA, datasourceSchemaDTO.getId()));
            dsMap.put(datasourceSchemaDTO.getId(), datasourceSchemaDTO);
            commonThreadPool.addTask(() -> {
                try {
                    initConnection(dsMap);
                } catch (SQLException | RuntimeException ignore) {
                    LogUtil.debug("Skip datasource schema warmup: " + datasourceSchemaDTO.getId());
                }
            });
        }
        LogUtil.info("dsMap size..." + coreDatasources.size());

    }

    public void update(DatasourceDTO datasourceDTO) throws CrestException {
        DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
        BeanUtils.copyBean(datasourceSchemaDTO, datasourceDTO);
        datasourceSchemaDTO.setSchemaAlias(String.format(SQLConstants.SCHEMA, datasourceSchemaDTO.getId()));
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        datasourceRequest.setDsList(Map.of(datasourceSchemaDTO.getId(), datasourceSchemaDTO));
        try {
            Connection conn = take();
            CalciteConnection calciteConnection = conn.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = buildSchema(datasourceRequest, calciteConnection);
        } catch (CrestException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            throw e;
        } catch (SQLException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
    }

    public void updateDsPoolAfterCheckStatus(DatasourceDTO datasourceDTO) throws CrestException {
        DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
        BeanUtils.copyBean(datasourceSchemaDTO, datasourceDTO);
        datasourceSchemaDTO.setSchemaAlias(String.format(SQLConstants.SCHEMA, datasourceSchemaDTO.getId()));
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        datasourceRequest.setDsList(Map.of(datasourceSchemaDTO.getId(), datasourceSchemaDTO));
        try {
            CalciteConnection calciteConnection = take().unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            if (rootSchema.getSubSchema(datasourceSchemaDTO.getSchemaAlias()) == null) {
                buildSchema(datasourceRequest, calciteConnection);
            }
            DatasourceConfiguration configuration = parseDatasourceConfiguration(datasourceDTO.getConfiguration(), datasourceDTO.getType());
            if (configuration.isUseSSH()) {
                Session session = Provider.getSessions().get(datasourceDTO.getId());
                if (session != null) {
                    session.disconnect();
                    Provider.getSessions().remove(datasourceDTO.getId());
                }
                startSshSession(configuration, null, datasourceDTO.getId());
            }
        } catch (CrestException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            throw e;
        } catch (SQLException | RuntimeException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(e.getMessage());
        }
    }

    public void delete(CoreDatasource datasource) throws CrestException {
        DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
        BeanUtils.copyBean(datasourceSchemaDTO, datasource);
        datasourceSchemaDTO.setSchemaAlias(String.format(SQLConstants.SCHEMA, datasourceSchemaDTO.getId()));
        try {
            Connection conn = take();
            CalciteConnection calciteConnection = conn.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            if (rootSchema.getSubSchema(datasourceSchemaDTO.getSchemaAlias()) != null) {
                JdbcSchema jdbcSchema = rootSchema.getSubSchema(datasourceSchemaDTO.getSchemaAlias()).unwrap(JdbcSchema.class);
                BasicDataSource basicDataSource = (BasicDataSource) jdbcSchema.getDataSource();
                basicDataSource.close();
                removeSubSchema(rootSchema, datasourceSchemaDTO.getSchemaAlias());
            }
        } catch (SQLException | RuntimeException e) {
            CrestException.throwException(e.getMessage());
        }
        Provider.getLPorts().remove(datasource.getId());
        if (Provider.getSessions().get(datasource.getId()) != null) {
            Provider.getSessions().get(datasource.getId()).disconnect();
        }
        Provider.getSessions().remove(datasource.getId());
    }

    public Connection take() {
        synchronized (this) {
            Connection currentConnection = calciteConnectionRef.get();
            if (isClosed(currentConnection)) {
                currentConnection = getCalciteConnection();
                calciteConnectionRef.set(currentConnection);
            }
            return currentConnection;
        }
    }

    @PreDestroy
    public void closeCalciteConnection() {
        Connection connection = calciteConnectionRef.getAndSet(null);
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            LogUtil.warn("Close calcite connection failed: " + e.getMessage());
        }
    }

    private boolean isClosed(Connection connection) {
        if (connection == null) {
            return true;
        }
        try {
            return connection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    private void removeSubSchema(SchemaPlus rootSchema, String schemaAlias) {
        CalciteSchema.from(rootSchema).removeSubSchema(schemaAlias);
    }

    private CoreDatasource selectPoolDatasource(Long dsId) {
        CoreDatasource engine = engineManage.engineDatasource();
        if (engine != null && Objects.equals(engine.getId(), dsId)) {
            return engine;
        }
        CoreDatasource datasource = coreDatasourceMapper.selectById(dsId);
        if (datasource == null || Arrays.asList("folder", "API", "Excel", "ExcelRemote").contains(datasource.getType())
                || !Strings.CI.equals(datasource.getStatus(), "Success")) {
            return null;
        }
        return datasource;
    }

    private void ensureSchemaRegistered(CalciteConnection calciteConnection, Long dsId) {
        SchemaPlus rootSchema = calciteConnection.getRootSchema();
        String schemaAlias = String.format(SQLConstants.SCHEMA, dsId);
        if (rootSchema.getSubSchema(schemaAlias) != null) {
            return;
        }
        CoreDatasource datasource = selectPoolDatasource(dsId);
        if (datasource == null) {
            return;
        }
        DatasourceSchemaDTO datasourceSchemaDTO = new DatasourceSchemaDTO();
        BeanUtils.copyBean(datasourceSchemaDTO, datasource);
        datasourceSchemaDTO.setSchemaAlias(schemaAlias);
        DatasourceRequest datasourceRequest = new DatasourceRequest();
        datasourceRequest.setDsList(Map.of(datasourceSchemaDTO.getId(), datasourceSchemaDTO));
        buildSchema(datasourceRequest, calciteConnection);
    }

    private Connection getConnectionFromPool(Long dsId) {
        try {
            Connection connection = take();
            CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConnection.getRootSchema();
            if (rootSchema.getSubSchema(String.format(SQLConstants.SCHEMA, dsId)) == null) {
                ensureSchemaRegistered(calciteConnection, dsId);
            }
            if (rootSchema.getSubSchema(String.format(SQLConstants.SCHEMA, dsId)) == null) {
                CrestException.throwException(Translator.get("i18n_check_datasource_connection"));
            }
            JdbcSchema jdbcSchema = rootSchema.getSubSchema(String.format(SQLConstants.SCHEMA, dsId)).unwrap(JdbcSchema.class);
            BasicDataSource basicDataSource = (BasicDataSource) jdbcSchema.getDataSource();
            basicDataSource.setMaxWaitMillis(5 * 1000);
            return basicDataSource.getConnection();
        } catch (CrestException e) {
            throw e;
        } catch (SQLException | RuntimeException e) {
            LogUtil.error(e.getMessage(), e);
            CrestException.throwException(Translator.get("i18n_invalid_connection") + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("java/sql-injection")
    public Map<String, Object> fetchResultField(EngineRequest engineRequest) {
        DatasourceConfiguration configuration = JsonUtil.parseObject(engineRequest.getEngine().getConfiguration(), DatasourceConfiguration.class);
        int queryTimeout = configuration.getQueryTimeout();
        DatasourceDTO datasource = new DatasourceDTO();
        BeanUtils.copyBean(datasource, engineRequest.getEngine());
        String readQuery = SqlSecurityPolicy.validateReadQuery(engineRequest.getQuery());
        try (Connection connection = getConnectionFromPool(datasource.getId());
             PreparedStatement preparedStatement = connection.prepareStatement(readQuery)) {
            preparedStatement.setQueryTimeout(queryTimeout);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("fields", fetchResultField(resultSet));
                map.put("data", dataResult(resultSet));
                return map;
            }
        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("java/sql-injection")
    public void exec(EngineRequest engineRequest) {
        DatasourceConfiguration configuration = JsonUtil.parseObject(engineRequest.getEngine().getConfiguration(), DatasourceConfiguration.class);
        int queryTimeout = configuration.getQueryTimeout();
        DatasourceDTO datasource = new DatasourceDTO();
        BeanUtils.copyBean(datasource, engineRequest.getEngine());
        String validatedSql = SqlSecurityPolicy.validateSingleStatement(engineRequest.getQuery());
        try (Connection connection = getConnectionFromPool(datasource.getId());
             PreparedStatement preparedStatement = connection.prepareStatement(validatedSql)) {
            preparedStatement.setQueryTimeout(queryTimeout);
            bindPreparedStatementValues(preparedStatement, engineRequest.getTableFieldWithValues());
            preparedStatement.execute();
        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
        }
    }

    public void execWithEngineTransaction(EngineRequest engineRequest, EngineConnectionExecutor executor) {
        DatasourceConfiguration configuration = JsonUtil.parseObject(engineRequest.getEngine().getConfiguration(), DatasourceConfiguration.class);
        int queryTimeout = configuration.getQueryTimeout();
        DatasourceDTO datasource = new DatasourceDTO();
        BeanUtils.copyBean(datasource, engineRequest.getEngine());
        try (Connection connection = getConnectionFromPool(datasource.getId())) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            Exception failure = null;
            try {
                executor.execute(connection, queryTimeout);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                failure = e;
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    failure.addSuppressed(rollbackError);
                }
            }
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException restoreError) {
                if (failure != null) {
                    failure.addSuppressed(restoreError);
                } else {
                    throw restoreError;
                }
            }
            if (failure != null) {
                if (failure instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                CrestException.throwException(failure);
            }
        } catch (SQLException e) {
            CrestException.throwException("SQL ERROR: " + e.getMessage());
        }
    }

    @FunctionalInterface
    public interface EngineConnectionExecutor {
        void execute(Connection connection, int queryTimeout) throws SQLException;
    }
}
