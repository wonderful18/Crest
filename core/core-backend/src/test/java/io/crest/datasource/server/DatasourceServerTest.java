package io.crest.datasource.server;

import io.crest.datasource.manage.DataSourceManage;
import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasourceServerTest {

    @Test
    void validateByIdRejectsMissingDatasourceWithoutInternalError() {
        DatasourceServer server = new DatasourceServer();
        DataSourceManage dataSourceManage = mock(DataSourceManage.class);
        ReflectionTestUtils.setField(server, "dataSourceManage", dataSourceManage);
        when(dataSourceManage.getCoreDatasource(999L)).thenReturn(null);

        CrestException exception = assertThrows(CrestException.class, () -> server.validate(999L));

        assertEquals("数据源不存在！", exception.getMsg());
    }
}
