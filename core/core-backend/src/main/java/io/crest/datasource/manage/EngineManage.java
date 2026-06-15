package io.crest.datasource.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.entity.CoreEngine;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.crest.datasource.dao.auto.mapper.CoreEngineMapper;
import io.crest.datasource.type.H2;
import io.crest.datasource.type.Mysql;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasourceDTO;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.factory.ProviderFactory;
import io.crest.result.ResultMessage;
import io.crest.utils.BeanUtils;
import io.crest.utils.JsonUtil;
import io.crest.utils.ModelUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Transactional(rollbackFor = Exception.class)
@SuppressWarnings("unchecked")
// 管理数据引擎配置、校验和内置演示数据源初始化。
public class EngineManage {
    private static final Long DEMO_RETAIL_DATASOURCE_ID = 910001L;
    private static final String DEMO_RETAIL_DATABASE = "crest_demo_retail";
    private static final Pattern MYSQL_JDBC_PATTERN = Pattern.compile("jdbc:mysql://([^:/?]+):(\\d+)/([^?]+)(?:\\?(.*))?");

    @Resource
    private Environment env;
    @Resource
    private CoreEngineMapper engineMapper;
    @Resource
    private CoreDatasourceMapper coreDatasourceMapper;

    @Value("${crest.path.engine:jdbc:h2:/opt/crest/desktop_data;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DATABASE_TO_UPPER=FALSE}")
    private String engineUrl;

    @Value("${crest.demo.enabled:true}")
    private boolean demoEnabled;

    // 查询当前数据引擎配置。
    public CoreEngine info() throws CrestException {
        List<CoreEngine> engines = engineMapper.selectList(null);
        if (CollectionUtils.isEmpty(engines)) {
            CrestException.throwException("未完整设置数据引擎");
        }
        return engines.get(0);
    }

    // 将当前引擎配置转换为数据源实体。
    public CoreDatasource getEngineDatasource() {
        List<CoreEngine> engines = engineMapper.selectList(null);
        if (CollectionUtils.isEmpty(engines)) {
            CrestException.throwException("未完整设置数据引擎");
        }
        CoreDatasource coreDatasource = new CoreDatasource();
        BeanUtils.copyBean(coreDatasource, engines.get(0));
        return coreDatasource;
    }


    // 查询当前引擎对应的数据源实体。
    public CoreDatasource engineDatasource() {
        List<CoreEngine> engines = engineMapper.selectList(null);
        CoreDatasource coreDatasource = new CoreDatasource();
        if (CollectionUtils.isEmpty(engines)) {
            return null;
        }
        BeanUtils.copyBean(coreDatasource, engines.get(0));
        return coreDatasource;
    }

    // 校验引擎连接配置。
    public void validate(CoreEngine engine) throws Exception {
        if (StringUtils.isEmpty(engine.getType()) || StringUtils.isEmpty(engine.getConfiguration())) {
            throw new Exception("未完整设置数据引擎");
        }
        try {

            DatasourceRequest datasourceRequest = new DatasourceRequest();
            DatasourceDTO datasource = new DatasourceDTO();
            BeanUtils.copyBean(datasource, engine);
            datasourceRequest.setDatasource(datasource);
            ProviderFactory.getProvider(engine.getType()).checkStatus(datasourceRequest);
        } catch (Exception e) {
            CrestException.throwException("校验失败：" + e.getMessage());
        }
    }

    // 保存数据引擎配置。
    public ResultMessage save(CoreEngine engine) throws Exception {
        if (engine.getId() == null) {
            engineMapper.insert(engine);
        } else {
            engineMapper.updateById(engine);
        }
        return ResultMessage.success(engine);
    }

    // 初始化默认本地数据引擎。
    public void initSimpleEngine() throws Exception {
        initLocalDataSource();
        QueryWrapper<CoreEngine> queryWrapper = new QueryWrapper<>();
        if (ModelUtils.isDesktop()) {
            queryWrapper.eq("type", engineType.h2.name());
        } else {
            queryWrapper.eq("type", engineType.mysql.name());
        }
        List<CoreEngine> engines = engineMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(engines) && validConfiguration(engines.get(0).getConfiguration())
                && !legacyCharsetConfiguration(engines.get(0).getConfiguration())) {
            return;
        }

        CoreEngine engine = CollectionUtils.isEmpty(engines) ? new CoreEngine() : engines.get(0);
        if (ModelUtils.isDesktop()) {
            engine.setType(engineType.h2.name());
            H2 h2 = new H2();
            h2.setJdbc(engineUrl);
            h2.setDataBase("PUBLIC");
            h2.setUsername(env.getProperty("spring.datasource.username"));
            h2.setPassword(env.getProperty("spring.datasource.password"));
            engine.setConfiguration(JsonUtil.toJSONString(h2).toString());
        } else {
            engine.setType(engineType.mysql.name());
            Mysql mysqlConfiguration = new Mysql();
            Matcher matcher = MYSQL_JDBC_PATTERN.matcher(env.getProperty("spring.datasource.url"));
            if (!matcher.find()) {
                return;
            }
            mysqlConfiguration.setHost(matcher.group(1));
            mysqlConfiguration.setPort(Integer.valueOf(matcher.group(2)));
            mysqlConfiguration.setDataBase(matcher.group(3));
            if (StringUtils.isNotBlank(matcher.group(4))) {
                mysqlConfiguration.setExtraParams(matcher.group(4));
            }
            mysqlConfiguration.setUsername(env.getProperty("spring.datasource.username"));
            mysqlConfiguration.setPassword(env.getProperty("spring.datasource.password"));
            engine.setConfiguration(JsonUtil.toJSONString(mysqlConfiguration).toString());
        }
        engine.setName("默认引擎");
        engine.setDescription("默认引擎");
        if (engine.getId() == null) {
            engineMapper.insert(engine);
        } else {
            engineMapper.updateById(engine);
        }
    }

    // 判断引擎配置是否为有效 JSON 配置。
    private boolean validConfiguration(String configuration) {
        return StringUtils.isNotBlank(configuration) && configuration.trim().startsWith("{");
    }

    // 判断引擎配置是否仍使用旧版字符集参数。
    private boolean legacyCharsetConfiguration(String configuration) {
        if (StringUtils.isBlank(configuration)) {
            return false;
        }
        String normalized = configuration.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("charactersetresults")
                || normalized.contains("characterencoding=utf8&");
    }


    // 内置引擎类型枚举。
    public enum engineType {
        mysql("Mysql"),
        h2("h2");
        private String alias;

        private engineType(String alias) {
            this.alias = alias;
        }

        // 查询引擎类型展示名称。
        public String getAlias() {
            return alias;
        }
    }

    // 初始化内置演示数据源。
    public void initLocalDataSource() {
        if (!demoEnabled) {
            return;
        }
        Matcher matcher = MYSQL_JDBC_PATTERN.matcher(env.getProperty("spring.datasource.url", ""));
        if (!matcher.find()) {
            return;
        }

        Mysql mysqlConfiguration = new Mysql();
        mysqlConfiguration.setType(engineType.mysql.name());
        mysqlConfiguration.setName(engineType.mysql.getAlias());
        mysqlConfiguration.setCatalog("OLTP");
        mysqlConfiguration.setHost(matcher.group(1));
        mysqlConfiguration.setPort(Integer.valueOf(matcher.group(2)));
        mysqlConfiguration.setDataBase(DEMO_RETAIL_DATABASE);
        if (StringUtils.isNotBlank(matcher.group(4))) {
            mysqlConfiguration.setExtraParams(matcher.group(4));
        }
        mysqlConfiguration.setUsername(env.getProperty("spring.datasource.username"));
        mysqlConfiguration.setPassword(env.getProperty("spring.datasource.password"));
        mysqlConfiguration.setUrlType("hostName");
        mysqlConfiguration.setInitialPoolSize(5);
        mysqlConfiguration.setMinPoolSize(5);
        mysqlConfiguration.setMaxPoolSize(20);
        mysqlConfiguration.setQueryTimeout(30);
        mysqlConfiguration.setUseSSH(false);

        long now = System.currentTimeMillis();
        CoreDatasource datasource = coreDatasourceMapper.selectById(DEMO_RETAIL_DATASOURCE_ID);
        CoreDatasource demoDatasource = new CoreDatasource();
        demoDatasource.setId(DEMO_RETAIL_DATASOURCE_ID);
        demoDatasource.setName("Crest 综合演示数据仓库");
        demoDatasource.setDescription("Crest 内置演示数据源，含零售经营与研发效能主题事实表、维表和指标视图。");
        demoDatasource.setType(engineType.mysql.name());
        demoDatasource.setPid(0L);
        demoDatasource.setEditType("0");
        demoDatasource.setConfiguration(JsonUtil.toJSONString(mysqlConfiguration).toString());
        demoDatasource.setUpdateTime(now);
        demoDatasource.setUpdateBy(1L);
        demoDatasource.setStatus("Success");
        demoDatasource.setTaskStatus("Success");
        demoDatasource.setEnableDataFill(false);
        if (datasource == null) {
            demoDatasource.setCreateTime(now);
            demoDatasource.setCreateBy("1");
            coreDatasourceMapper.insert(demoDatasource);
            return;
        }
        demoDatasource.setCreateTime(datasource.getCreateTime());
        demoDatasource.setCreateBy(datasource.getCreateBy());
        coreDatasourceMapper.updateById(demoDatasource);
    }
}
