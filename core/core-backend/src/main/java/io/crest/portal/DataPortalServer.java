package io.crest.portal;

import io.crest.exception.CrestException;
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

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) " + query.fromWhere,
                Long.class,
                query.args.toArray()
        );

        List<Object> listArgs = new ArrayList<>(query.args);
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
                       (SELECT COUNT(1) FROM bi_chart c WHERE c.scene_id = v.id) AS chart_count
                """ + query.fromWhere + " ORDER BY v.update_time " + orderDirection + " LIMIT ? OFFSET ?";
        List<DataPortalResourceVO> records = jdbcTemplate.query(listSql, (rs, rowNum) -> {
            DataPortalResourceVO vo = new DataPortalResourceVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setType(rs.getString("type"));
            vo.setExtFlag(rs.getInt("ext_flag"));
            vo.setUpdateTime(rs.getLong("update_time"));
            vo.setCreatorName(rs.getString("creator_name"));
            vo.setChartCount(rs.getInt("chart_count"));
            return vo;
        }, listArgs.toArray());

        DataPortalPageVO page = new DataPortalPageVO();
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
        String resourceSql = """
                SELECT v.id,
                       v.name,
                       v.type,
                       v.mobile_layout AS ext_flag,
                       v.update_time,
                       COALESCE(u.name, v.create_by) AS creator_name,
                       (SELECT COUNT(1) FROM bi_chart c WHERE c.scene_id = v.id) AS chart_count
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
            return vo;
        }, query.args.toArray());
        if (resources.isEmpty()) {
            CrestException.throwException("资源不存在或无访问权限");
        }
        return resources.get(0);
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
        return normalized;
    }

    private QueryParts buildBaseQuery(Long uid, DataPortalResourceRequest request) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                FROM bi_visualization v
                LEFT JOIN iam_user u ON u.id = CAST(v.create_by AS UNSIGNED)
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

    private static class QueryParts {
        private String fromWhere;
        private final List<Object> args;

        private QueryParts(String fromWhere, List<Object> args) {
            this.fromWhere = fromWhere;
            this.args = args;
        }
    }
}
