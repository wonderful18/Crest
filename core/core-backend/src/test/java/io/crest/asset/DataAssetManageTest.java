package io.crest.asset;

import io.crest.asset.dto.DataAssetImpactItemVO;
import io.crest.asset.dto.DataAssetImpactVO;
import io.crest.asset.dto.DataAssetProfileRequest;
import io.crest.asset.dto.DataAssetRequest;
import io.crest.asset.dto.DataAssetVO;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.IDUtils;
import io.crest.utils.SnowFlake;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class DataAssetManageTest {

    @Test
    void saveProfileAcceptsManagedNonDatasetAndClearsGovernanceFlags() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        setStaticField(IDUtils.class, "snowFlake", new SnowFlake(1, 1));
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        DataAssetVO asset = new DataAssetVO();
        asset.setAssetType(DataAssetUtils.TYPE_DATASOURCE);
        asset.setAssetId("910001");
        asset.setName("Crest 综合演示数据仓库");
        asset.setCreatorId(1L);
        doReturn(List.of(asset), List.of(asset), List.of()).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        DataAssetProfileRequest request = new DataAssetProfileRequest();
        request.setAssetType(DataAssetUtils.TYPE_DATASOURCE);
        request.setAssetId("910001");
        request.setDescription("核心数据源");
        request.setTags("生产");
        request.setOwnerId(1L);
        request.setCertified(true);
        request.setRecommended(true);
        request.setDeprecated(true);

        manage.saveProfile(request);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());
        Object[] args = argsCaptor.getValue();
        assertEquals(DataAssetUtils.TYPE_DATASOURCE, args[1]);
        assertEquals("910001", args[2]);
        assertEquals("核心数据源", args[3]);
        assertEquals(1L, args[4]);
        assertEquals(0, args[5]);
        assertEquals(0, args[6]);
        assertEquals(0, args[7]);
        assertEquals("生产", args[8]);
    }

    @Test
    void pageKeepsWhitespaceBetweenFiltersAndOrderBy() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        doReturn(List.<DataAssetVO>of()).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        DataAssetRequest request = new DataAssetRequest();
        request.setAssetType(DataAssetUtils.TYPE_DATASOURCE);
        request.setCertified(true);

        manage.page(1, 20, request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        assertFalse(sqlCaptor.getValue().contains("?ORDER BY"));
    }

    @Test
    void pageAppliesOwnerFilterWithExistingListFilters() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        doReturn(List.<DataAssetVO>of()).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        DataAssetRequest request = new DataAssetRequest();
        request.setAssetType(DataAssetUtils.TYPE_DATASET);
        request.setRecommended(true);
        request.setOwnerId(7L);

        manage.page(1, 20, request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), argsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("COALESCE(p.owner_id, a.creator_id) = ?"));
        assertEquals(List.of(DataAssetUtils.TYPE_DATASET, 1, 7L, 20, 0),
                Arrays.asList(argsCaptor.getValue()));
    }

    @Test
    void trustedFiltersExcludeDeprecatedAssets() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        doReturn(List.<DataAssetVO>of()).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        DataAssetRequest request = new DataAssetRequest();
        request.setCertified(true);
        request.setRecommended(true);

        manage.page(1, 20, request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("COALESCE(p.certified, 0) = ?"));
        assertTrue(sql.contains("COALESCE(p.recommended, 0) = ?"));
        assertEquals(2, sql.split("COALESCE\\(p.deprecated, 0\\) = 0", -1).length - 1);
    }

    @Test
    void pageEmbedsBackendPermissionScopesInUnifiedAssetSql() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenAnswer(invocation -> "SCOPE_" + invocation.getArgument(0, String.class));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
        doReturn(List.<DataAssetVO>of()).when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), any(Object[].class));

        manage.page(1, 20, new DataAssetRequest());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("SCOPE_datasource"));
        assertTrue(sql.contains("SCOPE_dataset"));
        assertTrue(sql.contains("SCOPE_panel"));
        assertTrue(sql.contains("SCOPE_screen"));
    }

    @Test
    void datasetImpactSummarizesChartsVisualizationsSharesAndCacheTasks() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        DataAssetManage manage = new DataAssetManage();
        setField(manage, "jdbcTemplate", jdbcTemplate);
        setField(manage, "platformPermissionManage", permissionManage);
        when(permissionManage.resourceScopeSql(anyString(), anyString(), anyString(), nullable(String.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);

        DataAssetVO asset = new DataAssetVO();
        asset.setAssetType(DataAssetUtils.TYPE_DATASET);
        asset.setAssetId("982004");
        asset.setName("角色分布透视");
        asset.setCreatorId(1L);
        doReturn(
                List.of(asset),
                List.of(impactItem(DataAssetUtils.TYPE_CHART, "980111")),
                List.of(impactItem(DataAssetUtils.TYPE_PANEL, "1259851055592771584")),
                List.of(impactItem(DataAssetUtils.TYPE_SHARE, "1262213725113421824")),
                List.of(impactItem(DataAssetUtils.TYPE_CACHE_TASK, "1262155861866975232"))
        ).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));

        DataAssetImpactVO impact = manage.impact(DataAssetUtils.TYPE_DATASET, "982004");

        assertEquals(4, impact.getItems().size());
        assertEquals(1L, impact.getSummary().get(DataAssetUtils.TYPE_CHART));
        assertEquals(1L, impact.getSummary().get(DataAssetUtils.TYPE_PANEL));
        assertEquals(1L, impact.getSummary().get(DataAssetUtils.TYPE_SHARE));
        assertEquals(1L, impact.getSummary().get(DataAssetUtils.TYPE_CACHE_TASK));
    }

    private static DataAssetImpactItemVO impactItem(String assetType, String assetId) {
        DataAssetImpactItemVO vo = new DataAssetImpactItemVO();
        vo.setAssetType(assetType);
        vo.setAssetTypeLabel(DataAssetUtils.assetTypeLabel(assetType));
        vo.setAssetId(assetId);
        vo.setName(assetId);
        vo.setRelation("使用该数据集");
        return vo;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = DataAssetManage.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setStaticField(Class<?> target, String name, Object value) {
        try {
            Field field = target.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
