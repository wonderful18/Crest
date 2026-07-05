package io.crest.system.server;

import io.crest.api.system.request.SQLBotConfigCreator;
import io.crest.api.system.vo.SettingItemVO;
import io.crest.constant.SystemSettingConstants;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.system.manage.SysParameterManage;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SysParameterServerAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void saveSqlBotConfigRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterServer server = new SysParameterServer();

        CrestException exception = assertThrows(CrestException.class,
                () -> server.saveSqlBotConfig(new SQLBotConfigCreator()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void sqlBotConfigRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterServer server = serverWith(mock(SysParameterManage.class));

        CrestException exception = assertThrows(CrestException.class, server::sqlBotConfig);

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void singleValueRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterManage manage = mock(SysParameterManage.class);
        when(manage.singleVal(SystemSettingConstants.INITIAL_PASSWORD)).thenReturn("InitialPassword123!");
        SysParameterServer server = serverWith(manage);

        CrestException exception = assertThrows(CrestException.class,
                () -> server.singleVal(SystemSettingConstants.INITIAL_PASSWORD));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void basicSettingsListRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterManage manage = mock(SysParameterManage.class);
        when(manage.groupList("basic.")).thenReturn(java.util.List.of());
        when(manage.convert(anyList())).thenReturn(java.util.List.of());
        SysParameterServer server = serverWith(manage);

        CrestException exception = assertThrows(CrestException.class, server::queryBasicSetting);

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void adminCanReadSingleValue() {
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        SysParameterManage manage = mock(SysParameterManage.class);
        when(manage.singleVal(SystemSettingConstants.DEFAULT_SORT)).thenReturn("1");
        SysParameterServer server = serverWith(manage);

        assertEquals("1", server.singleVal(SystemSettingConstants.DEFAULT_SORT));
    }

    @Test
    void requestTimeoutRemainsPublic() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterManage manage = mock(SysParameterManage.class);
        when(manage.groupList("basic.")).thenReturn(List.of());
        when(manage.convert(anyList())).thenReturn(List.of(setting(SystemSettingConstants.Front_Time_Out, "45")));
        SysParameterServer server = serverWith(manage);

        assertEquals(45, server.RequestTimeOut());
    }

    @Test
    void defaultSettingsRemainPublic() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SysParameterManage manage = mock(SysParameterManage.class);
        when(manage.groupList("basic.")).thenReturn(List.of());
        when(manage.convert(anyList())).thenReturn(List.of(
                setting(SystemSettingConstants.DEFAULT_SORT, "0"),
                setting(SystemSettingConstants.DEFAULT_OPEN, "1")
        ));
        SysParameterServer server = serverWith(manage);

        Map<String, Object> result = server.defaultSettings();

        assertEquals("0", result.get(SystemSettingConstants.DEFAULT_SORT));
        assertEquals("1", result.get(SystemSettingConstants.DEFAULT_OPEN));
    }

    private SysParameterServer serverWith(SysParameterManage manage) {
        SysParameterServer server = new SysParameterServer();
        ReflectionTestUtils.setField(server, "sysParameterManage", manage);
        return server;
    }

    private SettingItemVO setting(String key, String value) {
        SettingItemVO item = new SettingItemVO();
        item.setPkey(key);
        item.setPval(value);
        return item;
    }
}
