package io.crest.commons.utils;

import io.crest.api.permissions.user.vo.UserFormVO;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlParserUtilsSecurityTest {

    private SqlParserUtils newParser(UserFormVO userEntity) {
        SqlParserUtils parser = new SqlParserUtils();
        ReflectionTestUtils.setField(parser, "userEntity", userEntity);
        return parser;
    }

    private Map<Long, DatasourceSchemaDTO> dsMap() {
        Map<Long, DatasourceSchemaDTO> map = new HashMap<>();
        map.put(1L, new DatasourceSchemaDTO());
        return map;
    }

    @Test
    void systemVariableIsParameterizedNotConcatenated() {
        String injection = "evil' OR '1'='1";
        UserFormVO user = new UserFormVO();
        user.setName(injection);
        SqlParserUtils parser = newParser(user);

        SqlVariableHandleResult result = parser.handleVariableDefaultValueWithPreparedParams(
                "select * from t where name = $CREST_PARAM{$crest[sysParams.userName]}",
                null, false, false, null, true, dsMap(), null, user);

        String sql = result.getSql();
        assertTrue(sql.contains("?"), "系统变量必须以占位符 ? 绑定，而非拼接进 SQL");
        assertFalse(sql.contains(injection), "注入内容绝不能以明文出现在最终 SQL 中");
        assertFalse(sql.contains("OR '1'='1"));
        assertNotNull(result.getTableFieldWithValues());
        assertEquals(1, result.getTableFieldWithValues().size());
        assertEquals(injection, result.getTableFieldWithValues().get(0).getValue());
        assertEquals(Types.VARCHAR, result.getTableFieldWithValues().get(0).getType());
    }

    @Test
    void systemVariableInjectionPayloadNeverReachesSqlText() {
        String injection = "x'); drop table users; --";
        UserFormVO user = new UserFormVO();
        user.setEmail(injection);
        SqlParserUtils parser = newParser(user);

        SqlVariableHandleResult result = parser.handleVariableDefaultValueWithPreparedParams(
                "select * from t where email = $CREST_PARAM{$crest[sysParams.userEmail]}",
                null, false, false, null, true, dsMap(), null, user);

        String sql = result.getSql();
        assertTrue(sql.contains("?"), "系统变量必须以占位符 ? 绑定");
        assertFalse(sql.contains("drop table"), "DDL 注入绝不能进入 SQL 文本");
        assertFalse(sql.contains(injection), "注入内容绝不能以明文出现在最终 SQL 中");
        assertEquals(injection, result.getTableFieldWithValues().get(0).getValue());
    }
}
