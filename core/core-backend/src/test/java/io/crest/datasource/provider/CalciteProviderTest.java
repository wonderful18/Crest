package io.crest.datasource.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasourceDTO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalciteProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void initKeepsDefaultClassLoaderWhenDriverDirectoryIsUnavailable() throws Exception {
        Path notDirectory = Files.createFile(tempDir.resolve("drivers"));
        CalciteProvider provider = new CalciteProvider();
        ReflectionTestUtils.setField(provider, "FILE_PATH", notDirectory.toString());

        provider.init();

        assertNotNull(provider.extendedJdbcClassLoader);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closeCalciteConnectionClosesSharedConnectionAndClearsReference() throws Exception {
        CalciteProvider provider = new CalciteProvider();
        Connection connection = mock(Connection.class);
        AtomicReference<Connection> reference = (AtomicReference<Connection>) ReflectionTestUtils.getField(provider, "calciteConnectionRef");
        reference.set(connection);

        provider.closeCalciteConnection();

        verify(connection).close();
        assertNull(reference.get());
    }

    @Test
    void getConnectionRejectsNonJsonDatasourceConfiguration() {
        CalciteProvider provider = new CalciteProvider();
        DatasourceDTO datasource = new DatasourceDTO();
        datasource.setType("mysql");
        datasource.setConfiguration("not-json");

        CrestException exception = assertThrows(CrestException.class, () -> provider.getConnection(datasource));

        assertEquals("数据源配置格式无效", exception.getMsg());
    }
}
