package io.crest.datasource.server;

import io.crest.api.ds.vo.DriveDTO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatasourceDriverServerAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void saveRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceDriverServer server = new DatasourceDriverServer();

        CrestException exception = assertThrows(CrestException.class, () -> server.save(new DriveDTO()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void updateRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceDriverServer server = new DatasourceDriverServer();

        CrestException exception = assertThrows(CrestException.class, () -> server.update(new DriveDTO()));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void deleteRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceDriverServer server = new DatasourceDriverServer();

        CrestException exception = assertThrows(CrestException.class, () -> server.delete("mysql"));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    @Test
    void uploadJarRequiresAdminBeforeReadingFile() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceDriverServer server = new DatasourceDriverServer();

        CrestException exception = assertThrows(CrestException.class, () -> server.uploadJar("mysql", null));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }
}
