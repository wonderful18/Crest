package io.crest.substitute.permissions.role;

import io.crest.api.permissions.role.dto.RoleCreator;
import io.crest.api.permissions.role.dto.RoleEditor;
import io.crest.api.permissions.role.vo.ExternalUserVO;
import io.crest.api.permissions.role.vo.RoleDetailVO;
import io.crest.api.permissions.role.vo.RoleVO;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import io.crest.utils.IDUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
// 管理角色查询、授权绑定和基础信息维护。
public class CrestRoleManage {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PlatformPermissionManage platformPermissionManage;

    // 查询当前组织角色列表。
    public List<RoleVO> query(String keyword) {
        Long oid = currentOid();
        return platformPermissionManage.roles(oid, keyword);
    }

    // 查询指定组织角色列表。
    public List<RoleVO> queryByOid(Long oid, String keyword) {
        return platformPermissionManage.roles(oid == null ? currentOid() : oid, keyword);
    }

    // 查询用户已绑定角色。
    public List<RoleVO> selectedForUser(Long uid, String keyword) {
        List<Object> args = new java.util.ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.name, r.readonly, r.system_role
                FROM iam_role r
                INNER JOIN iam_user_role ur ON ur.rid = r.id
                WHERE ur.uid = ?
                """);
        args.add(uid);
        if (StringUtils.isNotBlank(keyword)) {
            sql.append(" AND r.name LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY r.system_role DESC, r.id ASC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            RoleVO vo = new RoleVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setReadonly(rs.getBoolean("readonly"));
            vo.setRoot(rs.getBoolean("system_role"));
            return vo;
        }, args.toArray());
    }

    // 搜索可加入当前组织的外部用户。
    public ExternalUserVO searchExternalUser(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        Long oid = currentOid();
        List<ExternalUserVO> users = jdbcTemplate.query("""
                SELECT u.id, u.account, u.name, u.email, u.phone
                FROM iam_user u
                WHERE u.enable = 1
                  AND NOT EXISTS (
                      SELECT 1 FROM iam_user_org uo WHERE uo.uid = u.id AND uo.oid = ?
                  )
                  AND (u.account = ? OR u.email = ? OR u.name LIKE ?)
                ORDER BY u.create_time DESC
                LIMIT 1
                """, (rs, rowNum) -> {
            ExternalUserVO vo = new ExternalUserVO();
            vo.setUid(rs.getLong("id"));
            vo.setAccount(rs.getString("account"));
            vo.setName(rs.getString("name"));
            vo.setEmail(rs.getString("email"));
            vo.setPhone(rs.getString("phone"));
            return vo;
        }, oid, keyword.trim(), keyword.trim(), "%" + keyword.trim() + "%");
        return users.isEmpty() ? null : users.get(0);
    }

    // 创建角色。
    @Transactional
    public Long create(RoleCreator creator) {
        platformPermissionManage.requireSystemAdmin();
        if (creator == null || StringUtils.isBlank(creator.getName())) {
            CrestException.throwException("角色名称不能为空");
        }
        long id = IDUtils.snowID();
        long now = System.currentTimeMillis();
        jdbcTemplate.update("""
                INSERT INTO iam_role(id, oid, name, code, description, type_code, readonly, system_role, org_admin, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, creator.getOid() == null ? currentOid() : creator.getOid(), creator.getName().trim(), null, creator.getDesc(),
                creator.getTypeCode() == null ? 0 : creator.getTypeCode(), false, false, false, now, now);
        return id;
    }

    // 编辑角色基础信息。
    @Transactional
    public void edit(RoleEditor editor) {
        platformPermissionManage.requireSystemAdmin();
        if (editor == null || editor.getId() == null || StringUtils.isBlank(editor.getName())) {
            CrestException.throwException("角色名称不能为空");
        }
        jdbcTemplate.update("""
                UPDATE iam_role
                SET name = ?, description = ?, update_time = ?
                WHERE id = ? AND readonly = 0
                """, editor.getName().trim(), editor.getDesc(), System.currentTimeMillis(), editor.getId());
    }

    // 删除角色及其权限绑定。
    @Transactional
    public void delete(Long rid) {
        platformPermissionManage.requireSystemAdmin();
        if (rid == null || rid == PlatformPermissionManage.SYSTEM_ADMIN_ROLE_ID || rid == PlatformPermissionManage.MEMBER_ROLE_ID) {
            CrestException.throwException("内置角色不能删除");
        }
        jdbcTemplate.update("DELETE FROM iam_user_role WHERE rid = ?", rid);
        jdbcTemplate.update("DELETE FROM iam_role_menu_permission WHERE rid = ?", rid);
        jdbcTemplate.update("DELETE FROM iam_resource_permission WHERE target_type = 'role' AND target_id = ?", rid);
        jdbcTemplate.update("DELETE FROM iam_role WHERE id = ? AND readonly = 0", rid);
    }

    // 查询角色详情。
    public RoleDetailVO detail(Long rid) {
        List<RoleDetailVO> list = jdbcTemplate.query("""
                SELECT id, name, description, type_code FROM iam_role WHERE id = ? LIMIT 1
                """, (rs, rowNum) -> {
            RoleDetailVO vo = new RoleDetailVO();
            vo.setId(rs.getLong("id"));
            vo.setName(rs.getString("name"));
            vo.setDesc(rs.getString("description"));
            vo.setTypeCode(rs.getInt("type_code"));
            return vo;
        }, rid);
        return list.isEmpty() ? null : list.get(0);
    }

    // 绑定用户到角色。
    @Transactional
    public void mountUsers(Long rid, List<Long> uids) {
        platformPermissionManage.requireSystemAdmin();
        if (rid == null || uids == null) {
            return;
        }
        Long oid = currentOid();
        for (Long uid : uids) {
            platformPermissionManage.bindUserToOrg(uid, oid, false);
            platformPermissionManage.bindUserToRole(uid, oid, rid);
        }
    }

    // 从角色中解绑用户。
    @Transactional
    public void unmountUser(Long rid, Long uid) {
        platformPermissionManage.requireSystemAdmin();
        jdbcTemplate.update("DELETE FROM iam_user_role WHERE uid = ? AND rid = ?", uid, rid);
    }

    // 查询解绑用户前的绑定数量。
    public Integer beforeUnmountInfo(Long rid, Long uid) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM iam_user_role WHERE rid = ? AND uid = ?",
                Integer.class, rid, uid);
        return count == null ? 0 : count;
    }

    // 获取当前组织 ID。
    private Long currentOid() {
        if (AuthUtils.getUser() == null) {
            return PlatformPermissionManage.ROOT_ORG_ID;
        }
        return AuthUtils.getUser().getDefaultOid() == null
                ? PlatformPermissionManage.ROOT_ORG_ID
                : AuthUtils.getUser().getDefaultOid();
    }
}
