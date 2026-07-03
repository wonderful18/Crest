package io.crest.system.server;

import io.crest.api.system.request.SQLBotConfigCreator;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.utils.AuthUtils;
import io.crest.utils.ModelUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    @Test
    void i18nOptionsIgnoresMissingOptionalDirectory() throws Exception {
        setDeploymentMode("standalone");
        SysParameterServer server = new SysParameterServer();

        assertDoesNotThrow(server::i18nOptions);
    }

    private static void setDeploymentMode(String mode) throws Exception {
        Field field = ModelUtils.class.getDeclaredField("modelValue");
        field.setAccessible(true);
        field.set(null, mode);
    }
}
