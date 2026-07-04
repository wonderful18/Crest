package io.crest.datasource.server;

import io.crest.auth.bo.TokenUserBO;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.manage.DataSourceManage;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasourceRequest;
import io.crest.extensions.datasource.vo.DatasourceConfiguration;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasourceServerTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
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
                () -> server.invokeMethod(DatasourceConfiguration.DatasourceType.API.name(), "forName", DatasourceRequest.class, new DatasourceRequest()));

        assertEquals("Datasource API method not supported", exception.getMsg());
    }
}
