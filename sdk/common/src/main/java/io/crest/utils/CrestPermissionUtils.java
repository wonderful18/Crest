package io.crest.utils;

import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.result.ResultCode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Shared helpers for checking current-user permissions in community deployments.
 */
public class CrestPermissionUtils {

    /**
     * Returns whether the current user has administrator permissions.
     */
    public static boolean currentUserIsAdmin() {
        Long uid = currentUserId();
        if (ObjectUtils.isEmpty(uid)) {
            return false;
        }
        try {
            Object userManage = CommonBeanFactory.getBean("crestUserManage");
            Method method = CrestReflectionUtils.findMethod(userManage.getClass(), "isAdmin");
            Object result = ReflectionUtils.invokeMethod(method, userManage, uid);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Exception ignored) {
        }
        return uid == 1L;
    }

    /**
     * Throws a permission exception when the current user is not an administrator.
     */
    public static void requireAdmin() {
        if (!currentUserIsAdmin()) {
            CrestException.throwException(ResultCode.PERMISSION_NO_ACCESS.code(), "当前用户没有管理员权限");
        }
    }

    /**
     * Allows administrators or the original creator to access creator-scoped resources.
     */
    public static boolean canAccessCreator(String createBy) {
        if (currentUserIsAdmin()) {
            return true;
        }
        Long uid = currentUserId();
        return uid != null && Strings.CS.equals(String.valueOf(uid), StringUtils.trimToEmpty(createBy));
    }

    /**
     * Throws a permission exception when the current user cannot access the creator scope.
     */
    public static void requireCreator(String createBy) {
        if (currentUserId() == null) {
            return;
        }
        if (!canAccessCreator(createBy)) {
            CrestException.throwException(ResultCode.PERMISSION_NO_ACCESS.code(), "当前用户无权访问该资源");
        }
    }

    /**
     * Reads the current authenticated user ID from the token context.
     */
    public static Long currentUserId() {
        TokenUserBO user = AuthUtils.getUser();
        return user == null ? null : user.getUserId();
    }

    /**
     * Builds the SQL predicate used to hide resources owned by other users.
     */
    public static String communityScopeSql() {
        Long uid = currentUserId();
        if (ObjectUtils.isEmpty(uid) || currentUserIsAdmin()) {
            return null;
        }
        String safeUid = Strings.CS.replace(String.valueOf(uid), "'", "''");
        return """
                SELECT 1 FROM (
                  SELECT CAST(id AS CHAR) AS resource_id, create_by FROM data_source
                  UNION ALL SELECT CAST(id AS CHAR) AS resource_id, create_by FROM dataset_group
                  UNION ALL SELECT CAST(id AS CHAR) AS resource_id, create_by FROM bi_chart
                  UNION ALL SELECT CAST(id AS CHAR) AS resource_id, create_by FROM bi_visualization
                ) resource_scope
                WHERE resource_scope.resource_id = CAST(%s AS CHAR)
                  AND (resource_scope.create_by IS NULL OR resource_scope.create_by <> '""" + safeUid + "')";
    }
}
