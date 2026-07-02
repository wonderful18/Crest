package io.crest.dataset.manage;

import io.crest.api.dataset.dto.EnumObj;
import io.crest.api.dataset.dto.MultFieldValuesRequest;
import io.crest.api.dataset.dto.PreviewSqlDTO;
import io.crest.api.dataset.union.DatasetGroupInfoDTO;
import io.crest.api.dataset.union.UnionDTO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.datasource.dao.auto.entity.CoreDatasource;
import io.crest.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.crest.exception.CrestException;
import io.crest.extensions.datasource.dto.DatasetTableDTO;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatasetDataManageAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void tableFieldsRejectsUnauthorizedDatasourceBeforeMetadataFetch() {
        DatasetDataManage manage = datasetDataManageWithDatasource("3");
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);

        DatasetTableDTO table = table(10L);
        table.setType("db");
        table.setInfo("{\"table\":\"orders\"}");

        CrestException exception = assertThrows(CrestException.class, () -> manage.getTableFields(table));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verifyNoInteractions(datasetSQLManage);
    }

    @Test
    void previewSqlRejectsUnauthorizedDatasourceBeforeQueryBuild() {
        DatasetDataManage manage = datasetDataManageWithDatasource("3");
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);

        PreviewSqlDTO request = new PreviewSqlDTO();
        request.setDatasourceId(10L);
        request.setIsCross(false);
        request.setSql(Base64.getEncoder().encodeToString("SELECT 1".getBytes(StandardCharsets.UTF_8)));

        CrestException exception = assertThrows(CrestException.class, () -> manage.previewSql(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verifyNoInteractions(datasetSQLManage);
    }

    @Test
    void previewDataRejectsUnauthorizedUnionDatasourceBeforeSqlBuild() {
        DatasetDataManage manage = datasetDataManageWithDatasource("3");
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);

        CrestException exception = assertThrows(CrestException.class,
                () -> manage.previewDataWithLimit(dataset(10L), 0, 100, false, false));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verifyNoInteractions(datasetSQLManage);
    }

    @Test
    void fieldEnumDsRejectsUnauthorizedDatasourceBeforeSqlBuild() {
        DatasetDataManage manage = datasetDataManageWithDatasource("3");
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);

        EnumObj request = new EnumObj();
        request.setField(field());
        request.setDataset(dataset(10L));

        CrestException exception = assertThrows(CrestException.class, () -> manage.getFieldEnumDs(request));

        assertEquals("当前用户无权访问该资源", exception.getMsg());
        verifyNoInteractions(datasetSQLManage);
    }

    @Test
    void fieldEnumReturnsEmptyForMissingRequest() throws Exception {
        DatasetDataManage manage = new DatasetDataManage();

        assertEquals(List.of(), manage.getFieldEnum(null));
    }

    @Test
    void fieldValueTreeRejectsMissingFirstFieldBeforeSqlBuild() {
        DatasetDataManage manage = new DatasetDataManage();
        DatasetTableFieldManage datasetTableFieldManage = mock(DatasetTableFieldManage.class);
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetTableFieldManage", datasetTableFieldManage);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);

        MultFieldValuesRequest request = new MultFieldValuesRequest();
        request.setFieldIds(List.of(100L));

        assertThrows(CrestException.class, () -> manage.getFieldValueTree(request));
        verifyNoInteractions(datasetSQLManage);
    }

    @Test
    void previewDataAllowsAuthorizedDatasourceToReachSqlBuild() throws Exception {
        DatasetDataManage manage = datasetDataManageWithDatasource("2");
        DatasetSQLManage datasetSQLManage = mock(DatasetSQLManage.class);
        ReflectionTestUtils.setField(manage, "datasetSQLManage", datasetSQLManage);
        when(datasetSQLManage.getUnionSQLForEdit(any(), isNull()))
                .thenThrow(new IllegalStateException("sql-build"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> manage.previewDataWithLimit(dataset(10L), 0, 100, false, false));

        assertEquals("sql-build", exception.getMessage());
        verify(datasetSQLManage).getUnionSQLForEdit(any(), isNull());
    }

    private DatasetDataManage datasetDataManageWithDatasource(String createBy) {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        DatasetDataManage manage = new DatasetDataManage();
        DatasetGroupManage datasetGroupManage = new DatasetGroupManage();
        CoreDatasourceMapper coreDatasourceMapper = mock(CoreDatasourceMapper.class);
        CoreDatasource datasource = new CoreDatasource();
        datasource.setId(10L);
        datasource.setCreateBy(createBy);
        datasource.setType("mysql");
        datasource.setConfiguration("{}");
        when(coreDatasourceMapper.selectById(10L)).thenReturn(datasource);
        ReflectionTestUtils.setField(datasetGroupManage, "coreDatasourceMapper", coreDatasourceMapper);
        ReflectionTestUtils.setField(manage, "datasetGroupManage", datasetGroupManage);
        return manage;
    }

    private DatasetGroupInfoDTO dataset(Long datasourceId) {
        DatasetGroupInfoDTO dataset = new DatasetGroupInfoDTO();
        dataset.setIsCross(false);
        dataset.setAllFields(List.of(field()));

        UnionDTO union = new UnionDTO();
        union.setCurrentDs(table(datasourceId));
        dataset.setUnion(List.of(union));
        return dataset;
    }

    private DatasetTableDTO table(Long datasourceId) {
        DatasetTableDTO table = new DatasetTableDTO();
        table.setDatasourceId(datasourceId);
        table.setIsCross(false);
        return table;
    }

    private DatasetTableFieldDTO field() {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setId(100L);
        field.setName("订单编号");
        field.setOriginName("order_no");
        field.setEngineFieldName("order_no");
        field.setFieldShortName("order_no");
        field.setFieldType(0);
        return field;
    }
}
