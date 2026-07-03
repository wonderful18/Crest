package io.crest.system.server;

import io.crest.api.system.request.SQLBotConfigCreator;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
