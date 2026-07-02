package io.crest.portal;

import io.crest.commons.constants.OptConstants;
import io.crest.exception.CrestException;
import io.crest.operation.manage.CoreOptRecentManage;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/data-portal")
public class DataPortalServer {

    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DataPortalPermissionManage dataPortalPermissionManage;

    @Resource
    private CoreOptRecentManage coreOptRecentManage;

    @GetMapping("/overview")
    public DataPortalOverviewVO overview() {
        Long uid = dataPortalPermissionManage.currentUid();
        QueryParts query = buildBaseQuery(uid, null);
        List<Object> args = new ArrayList<>(query.args);
        DataPortalOverviewVO overview = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) AS total,
                       SUM(CASE WHEN v.type = 'dataV' THEN 1 ELSE 0 END) AS screen_count,
                       SUM(CASE WHEN v.type = 'dashboard' THEN 1 ELSE 0 END) AS dashboard_count,
                       MAX(v.update_time) AS latest_update_time
                """ + query.fromWhere, (rs, rowNum) -> {
            DataPortalOverviewVO vo = new DataPortalOverviewVO();
            vo.setTotal(rs.getLong("total"));
            vo.setScreenCount(rs.getLong("screen_count"));
            vo.setDashboardCount(rs.getLong("dashboard_count"));
            long latest = rs.getLong("latest_update_time");
            vo.setLatestUpdateTime(rs.wasNull() ? null : latest);
            vo.setBackendAccess(dataPortalPermissionManage.hasBackendAccess(uid));
            return vo;
        }, args.toArray());
        return overview == null ? new DataPortalOverviewVO() : overview;
    }

    @PostMapping("/resources")
    public DataPortalPageVO resources(@RequestBody(required = false) DataPortalResourceRequest request) {
        Long uid = dataPortalPermissionManage.currentUid();
        DataPortalResourceRequest normalized = normalizeRequest(request);
        QueryParts query = buildBaseQuery(uid, normalized);
        QueryParts countQuery = buildBaseQuery(uid, copyWithoutType(normalized));

        // query.fromWhere 只由固定 SQL 片段、白名单分支和 JDBC 参数占位符组成。
        // nosemgrep: java.spring.security.injection.tainted-sql-string.tainted-sql-string
        String countSql = "SELECT COUNT(1) " + query.fromWhere;
        long total = jdbcTemplate.queryForObject(
                countSql,
                Long.class,
                query.args.toArray()
        );
        DataPortalPageVO page = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) AS total,
                       SUM(CASE WHEN v.type = 'dataV' THEN 1 ELSE 0 END) AS screen_count,
                       SUM(CASE WHEN v.type = 'dashboard' THEN 1 ELSE 0 END) AS dashboard_count
                """ + countQuery.fromWhere, (rs, rowNum) -> {
            DataPortalPageVO vo = new DataPortalPageVO();
            vo.setTotal(total);
            vo.setScreenCount(rs.getLong("screen_count"));
            vo.setDashboardCount(rs.getLong("dashboard_count"));
            return vo;
        }, countQuery.args.toArray());
        if (page == null) {
            page = new DataPortalPageVO();
            page.setTotal(total);
        }

        List<Object> listArgs = new ArrayList<>();
        listArgs.add(uid);
        listArgs.addAll(query.args);
        listArgs.add(normalized.getPageSize());
        listArgs.add((normalized.getPage() - 1) * normalized.getPageSize());
        String orderDirection = Boolean.TRUE.equals(normalized.getAsc()) ? "ASC" : "DESC";
        String listSql = """
                SELECT v.id,
                       v.name,
                       v.type,
                       v.mobile_layout AS ext_flag,
                       v.update_time,
                       COALESCE(u.name, v.create_by) AS creator_name,
                       (SELECT COUNT(1) FROM bi_chart c WHERE c.scene_id = v.id) AS chart_count,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.certified = 1
                             AND p.deprecated = 0
                           WHERE c.scene_id = v.id
                       ) AS certified,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.recommended = 1
                             AND p.deprecated = 0
                           WHERE c.scene_id = v.id
                       ) AS recommended,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.deprecated = 1
                           WHERE c.scene_id = v.id
                       ) AS deprecated,
                       EXISTS (
                           SELECT 1
                           FROM workspace_favorite_resource fav
                           WHERE fav.resource_id = v.id
                             AND fav.uid = ?
                       ) AS favorite
                """ + query.fromWhere + " ORDER BY " + orderExpression(normalized, orderDirection) + " LIMIT ? OFFSET ?";
        List<DataPortalResourceVO> records = jdbcTemplate.query(listSql, (rs, rowNum) -> {
            DataPortalResourceVO vo = new DataPortalResourceVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setType(rs.getString("type"));
            vo.setExtFlag(rs.getInt("ext_flag"));
            vo.setUpdateTime(rs.getLong("update_time"));
            vo.setCreatorName(rs.getString("creator_name"));
            vo.setChartCount(rs.getInt("chart_count"));
            vo.setFavorite(rs.getBoolean("favorite"));
            vo.setCertified(rs.getBoolean("certified"));
            vo.setRecommended(rs.getBoolean("recommended"));
            vo.setDeprecated(rs.getBoolean("deprecated"));
            return vo;
        }, listArgs.toArray());

        page.setTotal(total);
        page.setPage(normalized.getPage());
        page.setPageSize(normalized.getPageSize());
        page.setRecords(records);
        return page;
    }

    @GetMapping("/resource/{id}")
    public DataPortalResourceVO resource(@PathVariable("id") Long id) {
        if (id == null) {
            CrestException.throwException("资源不存在");
        }
        DataPortalResourceRequest request = new DataPortalResourceRequest();
        request.setPage(1);
        request.setPageSize(1);
        Long uid = dataPortalPermissionManage.currentUid();
        QueryParts query = buildBaseQuery(uid, request);
        query.fromWhere += " AND v.id = ?";
        query.args.add(id);
        List<Object> resourceArgs = new ArrayList<>();
        resourceArgs.add(uid);
        resourceArgs.addAll(query.args);
        String resourceSql = """
                SELECT v.id,
                       v.name,
                       v.type,
                       v.mobile_layout AS ext_flag,
                       v.update_time,
                       COALESCE(u.name, v.create_by) AS creator_name,
                       (SELECT COUNT(1) FROM bi_chart c WHERE c.scene_id = v.id) AS chart_count,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.certified = 1
                             AND p.deprecated = 0
                           WHERE c.scene_id = v.id
                       ) AS certified,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.recommended = 1
                             AND p.deprecated = 0
                           WHERE c.scene_id = v.id
                       ) AS recommended,
                       EXISTS (
                           SELECT 1
                           FROM bi_chart c
                           INNER JOIN data_asset_profile p ON p.asset_type COLLATE utf8mb4_unicode_ci = 'dataset'
                             AND p.asset_id COLLATE utf8mb4_unicode_ci = CAST(c.table_id AS CHAR) COLLATE utf8mb4_unicode_ci
                             AND p.deprecated = 1
                           WHERE c.scene_id = v.id
                       ) AS deprecated,
                       EXISTS (
                           SELECT 1
                           FROM workspace_favorite_resource fav
                           WHERE fav.resource_id = v.id
                             AND fav.uid = ?
                       ) AS favorite
                """ + query.fromWhere + " LIMIT 1";
        List<DataPortalResourceVO> resources = jdbcTemplate.query(resourceSql, (rs, rowNum) -> {
            DataPortalResourceVO vo = new DataPortalResourceVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setType(rs.getString("type"));
            vo.setExtFlag(rs.getInt("ext_flag"));
            vo.setUpdateTime(rs.getLong("update_time"));
            vo.setCreatorName(rs.getString("creator_name"));
            vo.setChartCount(rs.getInt("chart_count"));
            vo.setFavorite(rs.getBoolean("favorite"));
            vo.setCertified(rs.getBoolean("certified"));
            vo.setRecommended(rs.getBoolean("recommended"));
            vo.setDeprecated(rs.getBoolean("deprecated"));
            return vo;
        }, resourceArgs.toArray());
        if (resources.isEmpty()) {
            CrestException.throwException("资源不存在或无访问权限");
        }
        DataPortalResourceVO resource = resources.get(0);
        recordRecent(resource);
        return resource;
    }

    private DataPortalResourceRequest normalizeRequest(DataPortalResourceRequest request) {
        DataPortalResourceRequest normalized = request == null ? new DataPortalResourceRequest() : request;
        int page = normalized.getPage() == null || normalized.getPage() < 1 ? 1 : normalized.getPage();
        int pageSize = normalized.getPageSize() == null || normalized.getPageSize() < 1 ? 24 : normalized.getPageSize();
        normalized.setPage(page);
        normalized.setPageSize(Math.min(pageSize, MAX_PAGE_SIZE));
        String type = StringUtils.trimToEmpty(normalized.getType());
        if (StringUtils.isBlank(type) || "all".equalsIgnoreCase(type)) {
            normalized.setType(null);
        } else {
            String normalizedType = type.toLowerCase(Locale.ROOT);
            if ("screen".equals(normalizedType)) {
                normalizedType = "datav";
            }
            if (!"datav".equals(normalizedType) && !"dashboard".equals(normalizedType)) {
                CrestException.throwException("资源类型不正确");
            }
            normalized.setType("datav".equals(normalizedType) ? "dataV" : "dashboard");
        }
        normalized.setKeyword(StringUtils.trimToNull(normalized.getKeyword()));
        String queryFrom = StringUtils.trimToEmpty(normalized.getQueryFrom()).toLowerCase(Locale.ROOT);
        if ("favorite".equals(queryFrom) || "favorites".equals(queryFrom)) {
            queryFrom = "store";
        }
        if (!"store".equals(queryFrom) && !"recent".equals(queryFrom)) {
            queryFrom = null;
        }
        normalized.setQueryFrom(queryFrom);
        return normalized;
    }

    private QueryParts buildBaseQuery(Long uid, DataPortalResourceRequest request) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                FROM bi_visualization v
                LEFT JOIN iam_user u ON u.id = CAST(v.create_by AS UNSIGNED)
                """);
        if (request != null && "store".equals(request.getQueryFrom())) {
            sql.append("""
                INNER JOIN workspace_favorite_resource wfr ON wfr.resource_id = v.id
                  AND wfr.uid = ?
                """);
            args.add(uid);
        } else if (request != null && "recent".equals(request.getQueryFrom())) {
            sql.append("""
                INNER JOIN workspace_recent_resource wrr ON wrr.resource_id = v.id
                  AND wrr.uid = ?
                """);
            args.add(uid);
        }
        sql.append("""
                WHERE v.delete_flag = 0
                  AND v.node_type = 'leaf'
                  AND v.status = 1
                  AND v.type IN ('dataV', 'dashboard')
                """);
        if (request != null && StringUtils.isNotBlank(request.getType())) {
            sql.append(" AND v.type = ?");
            args.add(request.getType());
        }
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            sql.append(" AND LOWER(v.name) LIKE LOWER(CONCAT('%', ?, '%'))");
            args.add(request.getKeyword());
        }
        dataPortalPermissionManage.appendReadScope(sql, args, uid);
        return new QueryParts(sql.toString(), args);
    }

    private DataPortalResourceRequest copyWithoutType(DataPortalResourceRequest source) {
        DataPortalResourceRequest copy = new DataPortalResourceRequest();
        copy.setKeyword(source.getKeyword());
        copy.setPage(source.getPage());
        copy.setPageSize(source.getPageSize());
        copy.setAsc(source.getAsc());
        copy.setQueryFrom(source.getQueryFrom());
        return copy;
    }

    private String orderExpression(DataPortalResourceRequest request, String direction) {
        if (request != null && "store".equals(request.getQueryFrom())) {
            return "wfr.time " + direction;
        }
        if (request != null && "recent".equals(request.getQueryFrom())) {
            return "wrr.time " + direction;
        }
        return "deprecated ASC, certified DESC, recommended DESC, v.update_time " + direction;
    }

    private void recordRecent(DataPortalResourceVO resource) {
        coreOptRecentManage.saveOpt(
                resource.getId(),
                OptConstants.OPT_RESOURCE_TYPE.VISUALIZATION,
                OptConstants.OPT_TYPE.UPDATE
        );
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
