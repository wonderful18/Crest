package io.crest.datasource.server;

import io.crest.auth.bo.TokenUserBO;
import io.crest.api.ds.vo.RemoteExcelRequest;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.manage.DataSourceManage;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasourceServerTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void validateByIdRejectsMissingDatasourceWithoutInternalError() {
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);
        when(dataSourceManage.getCoreDatasource(999L)).thenReturn(null);

        CrestException exception = assertThrows(CrestException.class, () -> server.validate(999L));

        assertEquals("数据源不存在！", exception.getMsg());
    }

    @Test
    void loadRemoteFileRequiresDatasourceAccessBeforeLoadingRemoteContent() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        RemoteExcelRequest request = new RemoteExcelRequest();
        request.setDatasourceId(10L);
        request.setUrl("https://example.com/data.xlsx");

        CrestException exception = assertThrows(CrestException.class, () -> server.loadRemoteFile(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void uploadFileRequiresDatasourceAccessBeforeParsingFile() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.csv",
                "text/csv",
                "name\nalice\n".getBytes());

        CrestException exception = assertThrows(CrestException.class, () -> server.uploadFile(file, 10L, 0));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void invokeMethodRejectsUnsupportedMethodNameBeforeReflection() {
        DatasourceServer server = new DatasourceServer();

        CrestException exception = assertThrows(CrestException.class,
                () -> server.invokeMethod("API", "forName", Object.class, new Object()));

        assertEquals("Datasource API method not supported", exception.getMsg());
    }

    @Test
    void tableStatusRequiresDatasourceAccessBeforeQueryingTasks() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);

        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy("3");
        when(dataSourceManage.getCoreDatasource(10L)).thenReturn(datasource);

        DatasetTableDTO request = new DatasetTableDTO();
        request.setDatasourceId(10L);

        CrestException exception = assertThrows(CrestException.class, () -> server.tableStatus(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
    }

    @Test
    void quoteIdentifierRejectsSqlControlCharactersInTableName() throws Exception {
        DatasourceServer server = new DatasourceServer();
        Method quoteIdentifier = DatasourceServer.class.getDeclaredMethod("quoteIdentifier", String.class);
        quoteIdentifier.setAccessible(true);

        assertEquals("`excel_sheet`", quoteIdentifier.invoke(server, "excel_sheet"));
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> quoteIdentifier.invoke(server, "excel_sheet; DROP TABLE iam_user"));

        assertTrue(exception.getCause() instanceof CrestException);
    }

    @Test
    void quoteExcelIdentifierEscapesFieldNameAsSingleIdentifier() throws Exception {
        DatasourceServer server = new DatasourceServer();
        Method quoteExcelIdentifier = DatasourceServer.class.getDeclaredMethod("quoteExcelIdentifier", String.class);
        quoteExcelIdentifier.setAccessible(true);

        String quoted = (String) quoteExcelIdentifier.invoke(server, "field` FROM t; DROP TABLE t; --");

        assertEquals("`field`` FROM t; DROP TABLE t; --`", quoted);
    }
}
