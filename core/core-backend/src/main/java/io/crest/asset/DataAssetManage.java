package io.crest.asset;

import io.crest.asset.dto.DataAssetDetailVO;
import io.crest.asset.dto.DataAssetImpactItemVO;
import io.crest.asset.dto.DataAssetImpactVO;
import io.crest.asset.dto.DataAssetOwnerVO;
import io.crest.asset.dto.DataAssetPageVO;
import io.crest.asset.dto.DataAssetProfileRequest;
import io.crest.asset.dto.DataAssetRequest;
import io.crest.asset.dto.DataAssetVO;
import io.crest.exception.CrestException;
import io.crest.result.ResultCode;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.utils.IDUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataAssetManage {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PlatformPermissionManage platformPermissionManage;

    public DataAssetPageVO page(Integer page, Integer pageSize, DataAssetRequest request) {
        int normalizedPage = DataAssetUtils.normalizePage(page);
        int normalizedPageSize = DataAssetUtils.normalizePageSize(pageSize);
        DataAssetRequest normalized = normalizeRequest(request);
        QueryParts query = filteredAssets(normalized);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) " + query.fromWhere, Long.class, query.args.toArray());
        List<Object> listArgs = new ArrayList<>(query.args);
        listArgs.add(normalizedPageSize);
        listArgs.add((normalizedPage - 1) * normalizedPageSize);
        List<DataAssetVO> records = jdbcTemplate.query("""
                SELECT a.asset_type,
                       a.asset_id,
                       a.name,
                       a.extra_type,
                       a.parent_asset_type,
                       a.parent_asset_id,
                       a.creator_id,
                       a.org_id,
                       a.create_time,
                       a.update_time,
                       p.description,
                       p.tags,
                       COALESCE(p.certified, 0) AS certified,
                       COALESCE(p.recommended, 0) AS recommended,
                       COALESCE(p.deprecated, 0) AS deprecated,
                       COALESCE(p.owner_id, a.creator_id) AS owner_id,
                       COALESCE(owner.name COLLATE utf8mb4_unicode_ci,
                                creator.name COLLATE utf8mb4_unicode_ci,
                                CAST(COALESCE(p.owner_id, a.creator_id) AS CHAR) COLLATE utf8mb4_unicode_ci) AS owner_name,
                       COALESCE(creator.name COLLATE utf8mb4_unicode_ci,
                                CAST(a.creator_id AS CHAR) COLLATE utf8mb4_unicode_ci) AS creator_name,
                       COALESCE(org.name COLLATE utf8mb4_unicode_ci,
                                '默认组织' COLLATE utf8mb4_unicode_ci) AS org_name
                """ + query.fromWhere + """

                ORDER BY COALESCE(p.deprecated, 0) ASC,
                         COALESCE(p.certified, 0) DESC,
                         COALESCE(p.recommended, 0) DESC,
                         a.update_time DESC,
                         a.asset_id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> mapAsset(rs), listArgs.toArray());
        records.forEach(this::fillAssetAccessAndCounts);

        DataAssetPageVO vo = new DataAssetPageVO();
        vo.setPage(normalizedPage);
        vo.setPageSize(normalizedPageSize);
        vo.setTotal(total == null ? 0 : total);
        vo.setRecords(records);
        return vo;
    }

    public DataAssetDetailVO detail(String assetType, String assetId) {
        DataAssetVO asset = loadAsset(assetType, assetId);
        DataAssetDetailVO detail = new DataAssetDetailVO();
        detail.setAsset(asset);
        detail.setUpstream(upstream(asset));
        detail.setDownstream(downstream(asset));
        detail.setImpact(impact(asset));
        return detail;
    }

    public DataAssetImpactVO impact(String assetType, String assetId) {
        return impact(loadAsset(assetType, assetId));
    }

    @Transactional
    public DataAssetDetailVO saveProfile(DataAssetProfileRequest request) {
        if (request == null) {
            CrestException.throwException("资产信息不能为空");
        }
        String assetType = DataAssetUtils.requireAssetType(request.getAssetType());
        String assetId = StringUtils.trimToNull(request.getAssetId());
        if (assetId == null) {
            CrestException.throwException("资产ID不能为空");
        }
        DataAssetVO asset = loadAsset(assetType, assetId);
        if (!Boolean.TRUE.equals(asset.getCanManage())) {
            CrestException.throwException(ResultCode.PERMISSION_NO_ACCESS.code(), "当前用户无权维护该资产");
        }
        String description = trimToLength(request.getDescription(), 1024, "资产说明");
        String tags = trimToLength(request.getTags(), 1024, "标签");
        DataAssetUtils.GovernanceStatus status = DataAssetUtils.TYPE_DATASET.equals(assetType)
                ? DataAssetUtils.normalizeGovernanceStatus(
                request.getCertified(), request.getRecommended(), request.getDeprecated())
                : DataAssetUtils.emptyGovernanceStatus();
        Long uid = currentUid();
        long now = System.currentTimeMillis();
        jdbcTemplate.update("""
                INSERT INTO data_asset_profile
                    (id, asset_type, asset_id, description, owner_id, certified, recommended, deprecated, tags, create_time, update_time, update_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    description = VALUES(description),
                    owner_id = VALUES(owner_id),
                    certified = VALUES(certified),
                    recommended = VALUES(recommended),
                    deprecated = VALUES(deprecated),
                    tags = VALUES(tags),
                    update_time = VALUES(update_time),
                    update_by = VALUES(update_by)
                """,
                IDUtils.snowID(),
                assetType,
                assetId,
                description,
                request.getOwnerId(),
                bool(status.certified()),
                bool(status.recommended()),
                bool(status.deprecated()),
                tags,
                now,
                now,
                uid);
        return detail(assetType, assetId);
    }

    public List<DataAssetOwnerVO> owners() {
        return jdbcTemplate.query("""
                SELECT id, name, account
                FROM iam_user
                WHERE enable = 1
                ORDER BY name ASC, account ASC
                """, (rs, rowNum) -> {
            DataAssetOwnerVO vo = new DataAssetOwnerVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setAccount(rs.getString("account"));
            return vo;
        });
    }

    private DataAssetRequest normalizeRequest(DataAssetRequest request) {
        DataAssetRequest normalized = request == null ? new DataAssetRequest() : request;
        normalized.setKeyword(StringUtils.trimToNull(normalized.getKeyword()));
        normalized.setAssetType(DataAssetUtils.normalizeAssetType(normalized.getAssetType()));
        return normalized;
    }

    private DataAssetVO loadAsset(String assetType, String assetId) {
        DataAssetRequest request = new DataAssetRequest();
        request.setAssetType(DataAssetUtils.requireAssetType(assetType));
        QueryParts query = filteredAssets(request);
        query.fromWhere += " AND a.asset_id COLLATE utf8mb4_unicode_ci = ? COLLATE utf8mb4_unicode_ci";
        query.args.add(StringUtils.trimToEmpty(assetId));
        List<DataAssetVO> assets = jdbcTemplate.query("""
                SELECT a.asset_type,
                       a.asset_id,
                       a.name,
                       a.extra_type,
                       a.parent_asset_type,
                       a.parent_asset_id,
                       a.creator_id,
                       a.org_id,
                       a.create_time,
                       a.update_time,
                       p.description,
                       p.tags,
                       COALESCE(p.certified, 0) AS certified,
                       COALESCE(p.recommended, 0) AS recommended,
                       COALESCE(p.deprecated, 0) AS deprecated,
                       COALESCE(p.owner_id, a.creator_id) AS owner_id,
                       COALESCE(owner.name COLLATE utf8mb4_unicode_ci,
                                creator.name COLLATE utf8mb4_unicode_ci,
                                CAST(COALESCE(p.owner_id, a.creator_id) AS CHAR) COLLATE utf8mb4_unicode_ci) AS owner_name,
                       COALESCE(creator.name COLLATE utf8mb4_unicode_ci,
                                CAST(a.creator_id AS CHAR) COLLATE utf8mb4_unicode_ci) AS creator_name,
                       COALESCE(org.name COLLATE utf8mb4_unicode_ci,
                                '默认组织' COLLATE utf8mb4_unicode_ci) AS org_name
                """ + query.fromWhere + " LIMIT 1", (rs, rowNum) -> mapAsset(rs), query.args.toArray());
        if (assets.isEmpty()) {
            CrestException.throwException("资产不存在或无访问权限");
        }
        DataAssetVO asset = assets.get(0);
        fillAssetAccessAndCounts(asset);
        return asset;
    }

    private QueryParts filteredAssets(DataAssetRequest request) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                FROM (
                """);
        sql.append(assetUnionSql());
        sql.append("""
                ) a
                LEFT JOIN data_asset_profile p
                    ON p.asset_type COLLATE utf8mb4_unicode_ci = a.asset_type COLLATE utf8mb4_unicode_ci
                   AND p.asset_id COLLATE utf8mb4_unicode_ci = a.asset_id COLLATE utf8mb4_unicode_ci
                LEFT JOIN iam_user owner ON owner.id = p.owner_id
                LEFT JOIN iam_user creator ON creator.id = a.creator_id
                LEFT JOIN iam_org org ON org.id = a.org_id
                WHERE 1 = 1
                """);
        if (request != null && StringUtils.isNotBlank(request.getAssetType())) {
            sql.append(" AND a.asset_type COLLATE utf8mb4_unicode_ci = ? COLLATE utf8mb4_unicode_ci");
            args.add(request.getAssetType());
        }
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            sql.append("""
                    AND (
                        LOWER(a.name COLLATE utf8mb4_unicode_ci) LIKE LOWER(CONCAT('%', ? COLLATE utf8mb4_unicode_ci, '%') COLLATE utf8mb4_unicode_ci)
                        OR LOWER(a.asset_id COLLATE utf8mb4_unicode_ci) LIKE LOWER(CONCAT('%', ? COLLATE utf8mb4_unicode_ci, '%') COLLATE utf8mb4_unicode_ci)
                        OR LOWER(COALESCE(p.tags COLLATE utf8mb4_unicode_ci, '' COLLATE utf8mb4_unicode_ci)) LIKE LOWER(CONCAT('%', ? COLLATE utf8mb4_unicode_ci, '%') COLLATE utf8mb4_unicode_ci)
                    )
                    """);
            args.add(request.getKeyword());
            args.add(request.getKeyword());
            args.add(request.getKeyword());
        }
        if (request != null && request.getCertified() != null) {
            sql.append(" AND COALESCE(p.certified, 0) = ?");
            args.add(bool(request.getCertified()));
            if (Boolean.TRUE.equals(request.getCertified())) {
                sql.append(" AND COALESCE(p.deprecated, 0) = 0");
            }
        }
        if (request != null && request.getRecommended() != null) {
            sql.append(" AND COALESCE(p.recommended, 0) = ?");
            args.add(bool(request.getRecommended()));
            if (Boolean.TRUE.equals(request.getRecommended())) {
                sql.append(" AND COALESCE(p.deprecated, 0) = 0");
            }
        }
        if (request != null && request.getDeprecated() != null) {
            sql.append(" AND COALESCE(p.deprecated, 0) = ?");
            args.add(bool(request.getDeprecated()));
        }
        if (request != null && request.getOwnerId() != null) {
            sql.append(" AND COALESCE(p.owner_id, a.creator_id) = ?");
            args.add(request.getOwnerId());
        }
        return new QueryParts(sql.toString(), args);
    }

    private String assetUnionSql() {
        List<String> selects = new ArrayList<>();
        selects.add("""
                SELECT 'datasource' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(ds.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       ds.name COLLATE utf8mb4_unicode_ci AS name,
                       ds.type COLLATE utf8mb4_unicode_ci AS extra_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       COALESCE(ds_ri.creator, CAST(ds.create_by AS UNSIGNED)) AS creator_id,
                       COALESCE(ds_ri.oid, 1) AS org_id,
                       ds.create_time AS create_time,
                       ds.update_time AS update_time
                FROM data_source ds
                LEFT JOIN iam_resource_index ds_ri
                    ON ds_ri.resource_type COLLATE utf8mb4_unicode_ci = 'datasource' COLLATE utf8mb4_unicode_ci
                   AND ds_ri.resource_id COLLATE utf8mb4_unicode_ci = CAST(ds.id AS CHAR) COLLATE utf8mb4_unicode_ci
                WHERE ds.type COLLATE utf8mb4_unicode_ci <> 'folder' COLLATE utf8mb4_unicode_ci
                """ + permissionClause(platformPermissionManage.resourceScopeSql("datasource", "ds.id", "ds.create_by", null)));
        selects.add("""
                SELECT 'table' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(dt.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       COALESCE(NULLIF(dt.name COLLATE utf8mb4_unicode_ci, '' COLLATE utf8mb4_unicode_ci),
                                dt.table_name COLLATE utf8mb4_unicode_ci,
                                CAST(dt.id AS CHAR) COLLATE utf8mb4_unicode_ci) AS name,
                       dt.type COLLATE utf8mb4_unicode_ci AS extra_type,
                       'dataset' COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(dg.id AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       COALESCE(dg_table_ri.creator, CAST(dg.create_by AS UNSIGNED)) AS creator_id,
                       COALESCE(dg_table_ri.oid, 1) AS org_id,
                       dg.create_time AS create_time,
                       dg.last_update_time AS update_time
                FROM dataset_table dt
                INNER JOIN dataset_group dg ON dg.id = dt.dataset_group_id AND dg.node_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                LEFT JOIN iam_resource_index dg_table_ri
                    ON dg_table_ri.resource_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                   AND dg_table_ri.resource_id COLLATE utf8mb4_unicode_ci = CAST(dg.id AS CHAR) COLLATE utf8mb4_unicode_ci
                WHERE 1 = 1
                """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)));
        selects.add("""
                SELECT 'dataset' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(dg.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       dg.name COLLATE utf8mb4_unicode_ci AS name,
                       dg.type COLLATE utf8mb4_unicode_ci AS extra_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       COALESCE(dg_ri.creator, CAST(dg.create_by AS UNSIGNED)) AS creator_id,
                       COALESCE(dg_ri.oid, 1) AS org_id,
                       dg.create_time AS create_time,
                       dg.last_update_time AS update_time
                FROM dataset_group dg
                LEFT JOIN iam_resource_index dg_ri
                    ON dg_ri.resource_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                   AND dg_ri.resource_id COLLATE utf8mb4_unicode_ci = CAST(dg.id AS CHAR) COLLATE utf8mb4_unicode_ci
                WHERE dg.node_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)));
        selects.add("""
                SELECT 'chart' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(c.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       COALESCE(NULLIF(c.title COLLATE utf8mb4_unicode_ci, '' COLLATE utf8mb4_unicode_ci),
                                CAST(c.id AS CHAR) COLLATE utf8mb4_unicode_ci) AS name,
                       c.type COLLATE utf8mb4_unicode_ci AS extra_type,
                       (CASE WHEN v.type COLLATE utf8mb4_unicode_ci = 'dataV' COLLATE utf8mb4_unicode_ci THEN 'screen' ELSE 'panel' END) COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(v.id AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       CAST(c.create_by AS UNSIGNED) AS creator_id,
                       COALESCE(CAST(NULLIF(v.org_id, '') AS UNSIGNED), 1) AS org_id,
                       c.create_time AS create_time,
                       c.update_time AS update_time
                FROM bi_chart c
                INNER JOIN bi_visualization v ON v.id = c.scene_id
                WHERE v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + permissionClause(visualizationScopeSql("v")));
        selects.add(visualizationSelect("panel", "dashboard"));
        selects.add(visualizationSelect("screen", "dataV"));
        selects.add("""
                SELECT 'share' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(s.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       CONCAT(COALESCE(v.name COLLATE utf8mb4_unicode_ci, '可视化资源' COLLATE utf8mb4_unicode_ci),
                              ' / ' COLLATE utf8mb4_unicode_ci,
                              s.uuid COLLATE utf8mb4_unicode_ci) AS name,
                       s.uuid COLLATE utf8mb4_unicode_ci AS extra_type,
                       (CASE WHEN v.type COLLATE utf8mb4_unicode_ci = 'dataV' COLLATE utf8mb4_unicode_ci THEN 'screen' ELSE 'panel' END) COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(v.id AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       s.creator AS creator_id,
                       s.oid AS org_id,
                       s.time AS create_time,
                       s.time AS update_time
                FROM share_link s
                INNER JOIN bi_visualization v ON v.id = s.resource_id
                WHERE v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + permissionClause(visualizationScopeSql("v")));
        return String.join("\nUNION ALL\n", selects);
    }

    private String visualizationSelect(String assetType, String visualizationType) {
        String resourceType = DataAssetUtils.TYPE_SCREEN.equals(assetType) ? "screen" : "panel";
        return """
                SELECT '%s' COLLATE utf8mb4_unicode_ci AS asset_type,
                       CAST(v.id AS CHAR) COLLATE utf8mb4_unicode_ci AS asset_id,
                       v.name COLLATE utf8mb4_unicode_ci AS name,
                       v.type COLLATE utf8mb4_unicode_ci AS extra_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_type,
                       CAST(NULL AS CHAR) COLLATE utf8mb4_unicode_ci AS parent_asset_id,
                       CAST(v.create_by AS UNSIGNED) AS creator_id,
                       COALESCE(CAST(NULLIF(v.org_id, '') AS UNSIGNED), 1) AS org_id,
                       v.create_time AS create_time,
                       v.update_time AS update_time
                FROM bi_visualization v
                WHERE v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                  AND v.type COLLATE utf8mb4_unicode_ci = '%s' COLLATE utf8mb4_unicode_ci
                """.formatted(assetType, visualizationType) +
                permissionClause(platformPermissionManage.resourceScopeSql(resourceType, "v.id", "v.create_by", "v.org_id"));
    }

    private void fillAssetAccessAndCounts(DataAssetVO asset) {
        asset.setAssetTypeLabel(DataAssetUtils.assetTypeLabel(asset.getAssetType()));
        asset.setCanManage(canManage(asset));
        asset.setUpstreamCount(upstreamCount(asset));
        asset.setDownstreamCount(downstreamCount(asset));
    }

    private boolean canManage(DataAssetVO asset) {
        Long uid = currentUid();
        if (uid == null || platformPermissionManage.isSystemAdmin(uid)) {
            return true;
        }
        if (asset.getCreatorId() != null && asset.getCreatorId().equals(uid)) {
            return true;
        }
        if (asset.getOrgId() != null && platformPermissionManage.isOrgAdmin(uid, asset.getOrgId())) {
            return true;
        }
        String resourceType = DataAssetUtils.resourceTypeForAsset(asset.getAssetType());
        String resourceId = asset.getAssetId();
        if (resourceType == null && StringUtils.isNotBlank(asset.getParentAssetType())) {
            resourceType = DataAssetUtils.resourceTypeForAsset(asset.getParentAssetType());
            resourceId = asset.getParentAssetId();
        }
        return resourceType != null
                && StringUtils.isNotBlank(resourceId)
                && platformPermissionManage.resourceWeight(resourceType, resourceId,
                asset.getCreatorId() == null ? null : String.valueOf(asset.getCreatorId()), asset.getOrgId()) >= 7;
    }

    private List<DataAssetImpactItemVO> upstream(DataAssetVO asset) {
        Long id = longId(asset.getAssetId());
        if (id == null) {
            return List.of();
        }
        return switch (asset.getAssetType()) {
            case DataAssetUtils.TYPE_TABLE -> queryItems("""
                    SELECT 'dataset' AS asset_type, CAST(dg.id AS CHAR) AS asset_id, dg.name AS name,
                           '所属数据集' AS relation, dg.last_update_time AS update_time
                    FROM dataset_table dt
                    INNER JOIN dataset_group dg ON dg.id = dt.dataset_group_id
                    WHERE dt.id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_DATASET -> datasetUpstream(id);
            case DataAssetUtils.TYPE_CHART -> queryItems("""
                    SELECT 'dataset' AS asset_type, CAST(dg.id AS CHAR) AS asset_id, dg.name AS name,
                           '图表使用的数据集' AS relation, dg.last_update_time AS update_time
                    FROM bi_chart c
                    INNER JOIN dataset_group dg ON dg.id = c.table_id
                    WHERE c.id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_PANEL, DataAssetUtils.TYPE_SCREEN -> panelUpstream(id);
            case DataAssetUtils.TYPE_SHARE -> shareUpstream(id);
            default -> List.of();
        };
    }

    private List<DataAssetImpactItemVO> downstream(DataAssetVO asset) {
        Long id = longId(asset.getAssetId());
        if (id == null) {
            return List.of();
        }
        return switch (asset.getAssetType()) {
            case DataAssetUtils.TYPE_DATASOURCE -> datasourceDownstream(id);
            case DataAssetUtils.TYPE_DATASET -> datasetDownstream(id);
            case DataAssetUtils.TYPE_PANEL, DataAssetUtils.TYPE_SCREEN -> visualizationDownstream(id);
            default -> List.of();
        };
    }

    private DataAssetImpactVO impact(DataAssetVO asset) {
        DataAssetImpactVO vo = new DataAssetImpactVO();
        vo.setAssetType(asset.getAssetType());
        vo.setAssetId(asset.getAssetId());
        List<DataAssetImpactItemVO> items = downstream(asset);
        vo.setItems(items);
        Map<String, Long> summary = new LinkedHashMap<>();
        for (DataAssetImpactItemVO item : items) {
            summary.merge(item.getAssetType(), 1L, Long::sum);
        }
        vo.setSummary(summary);
        return vo;
    }

    private List<DataAssetImpactItemVO> datasetUpstream(Long datasetId) {
        List<DataAssetImpactItemVO> items = new ArrayList<>();
        items.addAll(queryItems("""
                SELECT DISTINCT 'datasource' AS asset_type, CAST(ds.id AS CHAR) AS asset_id, ds.name AS name,
                       '提供数据表' AS relation, ds.update_time AS update_time
                FROM data_source ds
                INNER JOIN dataset_table dt ON dt.datasource_id = ds.id
                WHERE dt.dataset_group_id = ?
                  AND ds.type COLLATE utf8mb4_unicode_ci <> 'folder' COLLATE utf8mb4_unicode_ci
                """ + permissionClause(platformPermissionManage.resourceScopeSql("datasource", "ds.id", "ds.create_by", null)), datasetId));
        items.addAll(queryItems("""
                SELECT 'table' AS asset_type, CAST(dt.id AS CHAR) AS asset_id,
                       COALESCE(NULLIF(dt.name, ''), dt.table_name, CAST(dt.id AS CHAR)) AS name,
                       '组成数据集' AS relation, NULL AS update_time
                FROM dataset_table dt
                WHERE dt.dataset_group_id = ?
                ORDER BY dt.id ASC
                """, datasetId));
        return items;
    }

    private List<DataAssetImpactItemVO> datasetDownstream(Long datasetId) {
        List<DataAssetImpactItemVO> items = new ArrayList<>();
        String visualizationScope = permissionClause(visualizationScopeSql("v"));
        items.addAll(queryItems("""
                SELECT 'chart' AS asset_type, CAST(c.id AS CHAR) AS asset_id,
                       COALESCE(NULLIF(c.title, ''), CAST(c.id AS CHAR)) AS name,
                       '使用该数据集' AS relation, c.update_time AS update_time
                FROM bi_chart c
                INNER JOIN bi_visualization v ON v.id = c.scene_id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope + """
                ORDER BY c.update_time DESC
                """, datasetId));
        items.addAll(queryItems("""
                SELECT CASE WHEN v.type COLLATE utf8mb4_unicode_ci = 'dataV' COLLATE utf8mb4_unicode_ci THEN 'screen' ELSE 'panel' END AS asset_type,
                       CAST(v.id AS CHAR) AS asset_id,
                       v.name AS name,
                       '包含使用该数据集的图表' AS relation,
                       v.update_time AS update_time
                FROM bi_visualization v
                INNER JOIN bi_chart c ON c.scene_id = v.id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope + """
                GROUP BY v.id, v.type, v.name, v.update_time
                ORDER BY v.update_time DESC
                """, datasetId));
        items.addAll(queryItems("""
                SELECT 'share' AS asset_type,
                       CAST(s.id AS CHAR) AS asset_id,
                       CONCAT(v.name, ' / ', s.uuid) AS name,
                       '分享了受影响资源' AS relation,
                       s.time AS update_time
                FROM share_link s
                INNER JOIN bi_visualization v ON v.id = s.resource_id
                INNER JOIN bi_chart c ON c.scene_id = v.id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope + """
                GROUP BY s.id, v.name, s.uuid, s.time
                ORDER BY s.time DESC
                """, datasetId));
        items.addAll(queryItems("""
                SELECT 'cacheTask' AS asset_type,
                       CAST(t.id AS CHAR) AS asset_id,
                       COALESCE(NULLIF(t.name, ''), '数据集缓存任务') AS name,
                       '该数据集的缓存任务' AS relation,
                       t.update_time AS update_time
                FROM dataset_sync_task t
                WHERE t.dataset_group_id = ?
                """, datasetId));
        return items;
    }

    private List<DataAssetImpactItemVO> datasourceDownstream(Long datasourceId) {
        return queryItems("""
                SELECT DISTINCT 'dataset' AS asset_type,
                       CAST(dg.id AS CHAR) AS asset_id,
                       dg.name AS name,
                       '引用该数据源的数据集' AS relation,
                       dg.last_update_time AS update_time
                FROM dataset_group dg
                INNER JOIN dataset_table dt ON dt.dataset_group_id = dg.id
                WHERE dg.node_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                  AND dt.datasource_id = ?
                """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), datasourceId);
    }

    private List<DataAssetImpactItemVO> panelUpstream(Long visualizationId) {
        List<DataAssetImpactItemVO> items = new ArrayList<>();
        items.addAll(queryItems("""
                SELECT 'chart' AS asset_type, CAST(c.id AS CHAR) AS asset_id,
                       COALESCE(NULLIF(c.title, ''), CAST(c.id AS CHAR)) AS name,
                       '属于当前资源' AS relation,
                       c.update_time AS update_time
                FROM bi_chart c
                WHERE c.scene_id = ?
                ORDER BY c.update_time DESC
                """, visualizationId));
        items.addAll(queryItems("""
                SELECT DISTINCT 'dataset' AS asset_type, CAST(dg.id AS CHAR) AS asset_id,
                       dg.name AS name,
                       '当前资源使用的数据集' AS relation,
                       dg.last_update_time AS update_time
                FROM bi_chart c
                INNER JOIN dataset_group dg ON dg.id = c.table_id
                WHERE c.scene_id = ?
                """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), visualizationId));
        return items;
    }

    private List<DataAssetImpactItemVO> visualizationDownstream(Long visualizationId) {
        return queryItems("""
                SELECT 'share' AS asset_type,
                       CAST(s.id AS CHAR) AS asset_id,
                       s.uuid AS name,
                       '分享当前资源' AS relation,
                       s.time AS update_time
                FROM share_link s
                WHERE s.resource_id = ?
                ORDER BY s.time DESC
                """, visualizationId);
    }

    private List<DataAssetImpactItemVO> shareUpstream(Long shareId) {
        return queryItems("""
                SELECT CASE WHEN v.type COLLATE utf8mb4_unicode_ci = 'dataV' COLLATE utf8mb4_unicode_ci THEN 'screen' ELSE 'panel' END AS asset_type,
                       CAST(v.id AS CHAR) AS asset_id,
                       v.name AS name,
                       '被分享资源' AS relation,
                       v.update_time AS update_time
                FROM share_link s
                INNER JOIN bi_visualization v ON v.id = s.resource_id
                WHERE s.id = ?
                """ + permissionClause(visualizationScopeSql("v")), shareId);
    }

    private Integer upstreamCount(DataAssetVO asset) {
        Long id = longId(asset.getAssetId());
        if (id == null) {
            return 0;
        }
        return switch (asset.getAssetType()) {
            case DataAssetUtils.TYPE_TABLE -> count("""
                    SELECT COUNT(1)
                    FROM dataset_table dt
                    INNER JOIN dataset_group dg ON dg.id = dt.dataset_group_id
                    WHERE dt.id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_DATASET -> count("""
                    SELECT COUNT(DISTINCT ds.id)
                FROM data_source ds
                INNER JOIN dataset_table dt ON dt.datasource_id = ds.id
                WHERE dt.dataset_group_id = ?
                      AND ds.type COLLATE utf8mb4_unicode_ci <> 'folder' COLLATE utf8mb4_unicode_ci
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("datasource", "ds.id", "ds.create_by", null)), id)
                    + count("SELECT COUNT(1) FROM dataset_table dt WHERE dt.dataset_group_id = ?", id);
            case DataAssetUtils.TYPE_CHART -> count("""
                    SELECT COUNT(1)
                    FROM bi_chart c
                    INNER JOIN dataset_group dg ON dg.id = c.table_id
                    WHERE c.id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_PANEL, DataAssetUtils.TYPE_SCREEN -> count("SELECT COUNT(1) FROM bi_chart c WHERE c.scene_id = ?", id)
                    + count("""
                    SELECT COUNT(DISTINCT dg.id)
                    FROM bi_chart c
                    INNER JOIN dataset_group dg ON dg.id = c.table_id
                    WHERE c.scene_id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_SHARE -> count("""
                    SELECT COUNT(1)
                    FROM share_link s
                    INNER JOIN bi_visualization v ON v.id = s.resource_id
                    WHERE s.id = ?
                    """ + permissionClause(visualizationScopeSql("v")), id);
            default -> 0;
        };
    }

    private Integer downstreamCount(DataAssetVO asset) {
        Long id = longId(asset.getAssetId());
        if (id == null) {
            return 0;
        }
        return switch (asset.getAssetType()) {
            case DataAssetUtils.TYPE_DATASOURCE -> count("""
                    SELECT COUNT(DISTINCT dg.id)
                    FROM dataset_group dg
                    INNER JOIN dataset_table dt ON dt.dataset_group_id = dg.id
                    WHERE dg.node_type COLLATE utf8mb4_unicode_ci = 'dataset' COLLATE utf8mb4_unicode_ci
                      AND dt.datasource_id = ?
                    """ + permissionClause(platformPermissionManage.resourceScopeSql("dataset", "dg.id", "dg.create_by", null)), id);
            case DataAssetUtils.TYPE_DATASET -> datasetDownstreamCount(id);
            case DataAssetUtils.TYPE_PANEL, DataAssetUtils.TYPE_SCREEN -> count("SELECT COUNT(1) FROM share_link s WHERE s.resource_id = ?", id);
            default -> 0;
        };
    }

    private Integer datasetDownstreamCount(Long datasetId) {
        String visualizationScope = permissionClause(visualizationScopeSql("v"));
        return count("""
                SELECT COUNT(DISTINCT c.id)
                FROM bi_chart c
                INNER JOIN bi_visualization v ON v.id = c.scene_id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope, datasetId)
                + count("""
                SELECT COUNT(DISTINCT v.id)
                FROM bi_visualization v
                INNER JOIN bi_chart c ON c.scene_id = v.id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope, datasetId)
                + count("""
                SELECT COUNT(DISTINCT s.id)
                FROM share_link s
                INNER JOIN bi_visualization v ON v.id = s.resource_id
                INNER JOIN bi_chart c ON c.scene_id = v.id
                WHERE c.table_id = ?
                  AND v.delete_flag = 0
                  AND v.node_type COLLATE utf8mb4_unicode_ci = 'leaf' COLLATE utf8mb4_unicode_ci
                """ + visualizationScope, datasetId)
                + count("SELECT COUNT(1) FROM dataset_sync_task t WHERE t.dataset_group_id = ?", datasetId);
    }

    private Integer count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value.intValue();
    }

    private List<DataAssetImpactItemVO> queryItems(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DataAssetImpactItemVO vo = new DataAssetImpactItemVO();
            vo.setAssetType(rs.getString("asset_type"));
            vo.setAssetTypeLabel(DataAssetUtils.assetTypeLabel(vo.getAssetType()));
            vo.setAssetId(rs.getString("asset_id"));
            vo.setName(rs.getString("name"));
            vo.setRelation(rs.getString("relation"));
            vo.setUpdateTime(nullableLong(rs, "update_time"));
            return vo;
        }, args);
    }

    private DataAssetVO mapAsset(ResultSet rs) throws SQLException {
        DataAssetVO vo = new DataAssetVO();
        vo.setAssetType(rs.getString("asset_type"));
        vo.setAssetId(rs.getString("asset_id"));
        vo.setName(rs.getString("name"));
        vo.setExtraType(rs.getString("extra_type"));
        vo.setParentAssetType(rs.getString("parent_asset_type"));
        vo.setParentAssetId(rs.getString("parent_asset_id"));
        vo.setCreatorId(nullableLong(rs, "creator_id"));
        vo.setOrgId(nullableLong(rs, "org_id"));
        vo.setCreateTime(nullableLong(rs, "create_time"));
        vo.setUpdateTime(nullableLong(rs, "update_time"));
        vo.setDescription(rs.getString("description"));
        vo.setTags(rs.getString("tags"));
        DataAssetUtils.GovernanceStatus governanceStatus = DataAssetUtils.normalizeGovernanceStatus(
                rs.getBoolean("certified"),
                rs.getBoolean("recommended"),
                rs.getBoolean("deprecated")
        );
        vo.setCertified(governanceStatus.certified());
        vo.setRecommended(governanceStatus.recommended());
        vo.setDeprecated(governanceStatus.deprecated());
        vo.setOwnerId(nullableLong(rs, "owner_id"));
        vo.setOwnerName(rs.getString("owner_name"));
        vo.setCreatorName(rs.getString("creator_name"));
        vo.setOrgName(rs.getString("org_name"));
        vo.setAssetTypeLabel(DataAssetUtils.assetTypeLabel(vo.getAssetType()));
        return vo;
    }

    private String visualizationScopeSql(String alias) {
        String panelScope = platformPermissionManage.resourceScopeSql("panel", alias + ".id", alias + ".create_by", alias + ".org_id");
        String screenScope = platformPermissionManage.resourceScopeSql("screen", alias + ".id", alias + ".create_by", alias + ".org_id");
        if (StringUtils.isBlank(panelScope) && StringUtils.isBlank(screenScope)) {
            return null;
        }
        if (StringUtils.isBlank(panelScope) || StringUtils.isBlank(screenScope)) {
            return StringUtils.defaultIfBlank(panelScope, screenScope);
        }
        return "((" + alias + ".type COLLATE utf8mb4_unicode_ci = 'dashboard' COLLATE utf8mb4_unicode_ci AND "
                + panelScope + ") OR (" + alias + ".type COLLATE utf8mb4_unicode_ci = 'dataV' COLLATE utf8mb4_unicode_ci AND "
                + screenScope + "))";
    }

    private String permissionClause(String scopeSql) {
        return StringUtils.isBlank(scopeSql) ? "" : "\n AND " + scopeSql;
    }

    private Long currentUid() {
        return AuthUtils.getUser() == null ? null : AuthUtils.getUser().getUserId();
    }

    private Long longId(String value) {
        try {
            return StringUtils.isBlank(value) ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private int bool(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private String trimToLength(String value, int max, String label) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed != null && trimmed.length() > max) {
            CrestException.throwException(label + "不能超过 " + max + " 个字符");
        }
        return trimmed;
    }

    private static class QueryParts {
        private String fromWhere;
        private final List<Object> args;

        private QueryParts(String fromWhere, List<Object> args) {
            this.fromWhere = fromWhere;
            this.args = args;
        }
    }
}
