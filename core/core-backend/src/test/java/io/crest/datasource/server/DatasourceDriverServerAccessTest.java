package io.crest.datasource.server;

import io.crest.api.ds.vo.DriveDTO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.datasource.dao.auto.entity.CoreDriver;
import io.crest.datasource.dao.auto.entity.CoreDriverJar;
import io.crest.datasource.dao.auto.mapper.CoreDriverJarMapper;
import io.crest.datasource.dao.auto.mapper.CoreDriverMapper;
import io.crest.exception.CrestException;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasourceDriverServerAccessTest {

    @TempDir
    Path tempDir;

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

    @Test
    void uploadJarRejectsNonZipJarContent() {
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        CoreDriverJarMapper jarMapper = mock(CoreDriverJarMapper.class);
        DatasourceDriverServer server = serverWithDriver(jarMapper);
        MockMultipartFile file = new MockMultipartFile(
                "jarFile",
                "driver.jar",
                "application/java-archive",
                "not a jar".getBytes());

        CrestException exception = assertThrows(CrestException.class, () -> server.uploadJar("mysql", file));

        assertEquals("驱动文件内容无效", exception.getMsg());
        verify(jarMapper, never()).insert(any(CoreDriverJar.class));
    }

    @Test
    void uploadJarAcceptsCaseInsensitiveJarExtensionWithZipContent() throws Exception {
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        CoreDriverJarMapper jarMapper = mock(CoreDriverJarMapper.class);
        when(jarMapper.selectList(any())).thenReturn(List.of());
        DatasourceDriverServer server = serverWithDriver(jarMapper);
        MockMultipartFile file = new MockMultipartFile(
                "jarFile",
                "driver.JAR",
                "application/java-archive",
                jarBytes());

        server.uploadJar("mysql", file);

        ArgumentCaptor<CoreDriverJar> captor = ArgumentCaptor.forClass(CoreDriverJar.class);
        verify(jarMapper).insert(captor.capture());
        assertEquals("driver.JAR", captor.getValue().getFileName());
        assertTrue(Files.isRegularFile(tempDir.resolve("mysql").resolve(captor.getValue().getTransName())));
    }

    private DatasourceDriverServer serverWithDriver(CoreDriverJarMapper jarMapper) {
        DatasourceDriverServer server = new DatasourceDriverServer();
        CoreDriverMapper driverMapper = mock(CoreDriverMapper.class);
        when(driverMapper.selectById("mysql")).thenReturn(new CoreDriver());
        ReflectionTestUtils.setField(server, "coreDriverMapper", driverMapper);
        ReflectionTestUtils.setField(server, "coreDriverJarMapper", jarMapper);
        ReflectionTestUtils.setField(server, "DRIVER_PATH", tempDir.toString());
        return server;
    }

    private byte[] jarBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes());
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
