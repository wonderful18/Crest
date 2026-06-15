package io.crest.datasource.type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Component("obMysql")
@SuppressWarnings("deprecation")
// 定义 OceanBase MySQL 模式数据源连接参数和元数据匹配规则
public class ObMysql extends DatasourceConfiguration {
    private String driver = "com.oceanbase.jdbc.Driver";
    private String extraParams = "characterEncoding=UTF-8&connectTimeout=5000&useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull";
    @JsonIgnore
    private List<String> illegalParameters = Arrays.asList(
            "maxAllowedPacket", "autoDeserialize", "queryInterceptors", "statementInterceptors",
            "detectCustomCollations", "allowloadlocalinfile", "allowUrlInLocalInfile",
            "allowLoadLocalInfileInPath", "allowMultiQueries", "java.naming.factory.initial",
            "java.naming.provider.url", "rmi", "ldap://", "ldaps://", "connectionProperties",
            "initSQL", "dns", "file", "ftp"
    );
    private List<String> showTableSqls = Arrays.asList("show tables");

    // 构建当前数据源的连接地址
    public String getJdbc() {
        if (StringUtils.isNoneEmpty(getUrlType()) && !Strings.CI.equals(getUrlType(), "hostName")) {
            validateJdbcUrl(getJdbcUrl());
            return getJdbcUrl();
        }
        String jdbcUrl = "jdbc:oceanbase://HOSTNAME:PORT/DATABASE"
                .replace("HOSTNAME", getLHost().trim())
                .replace("PORT", getLPort().toString().trim())
                .replace("DATABASE", getDataBase().trim());
        if (StringUtils.isNotBlank(getExtraParams())) {
            jdbcUrl = jdbcUrl + "?" + getExtraParams().trim();
        }
        validateJdbcUrl(jdbcUrl);
        return jdbcUrl;
    }

    // 校验 JDBC URL 和危险参数
    private void validateJdbcUrl(String jdbcUrl) {
        if (!Strings.CI.startsWith(jdbcUrl, "jdbc:oceanbase://")) {
            CrestException.throwException("Illegal jdbcUrl: " + jdbcUrl);
        }
        String decodedUrl = URLDecoder.decode(StringUtils.defaultString(jdbcUrl), StandardCharsets.UTF_8);
        String decodedExtraParams = URLDecoder.decode(StringUtils.defaultString(getExtraParams()), StandardCharsets.UTF_8);
        for (String illegalParameter : illegalParameters) {
            if (Strings.CI.contains(decodedUrl, illegalParameter)
                    || Strings.CI.contains(decodedExtraParams, illegalParameter)) {
                CrestException.throwException("Illegal parameter: " + illegalParameter);
            }
        }
    }
}
