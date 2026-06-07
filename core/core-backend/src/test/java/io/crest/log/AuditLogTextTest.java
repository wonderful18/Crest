package io.crest.log;

import io.crest.constant.LogOT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogTextTest {

    @Test
    @DisplayName("权限审计文案应识别当前 kebab-case 路由")
    void shouldDescribeCurrentPermissionRoutes() {
        assertEquals("查看业务资源权限",
                AuditLogText.description(LogOT.READ, "DATA", "/api/v1/auth/business-permissions"));
        assertEquals("配置业务资源权限",
                AuditLogText.description(LogOT.AUTHORIZE, "DATA", "/api/v1/auth/business-permissions"));
        assertEquals("查看菜单权限",
                AuditLogText.description(LogOT.READ, "MENU", "/api/v1/auth/menu-permissions"));
        assertEquals("配置菜单权限",
                AuditLogText.description(LogOT.AUTHORIZE, "MENU", "/api/v1/auth/menu-permissions"));
    }

    @Test
    @DisplayName("路径重命名后的系统路由仍应生成明确审计文案")
    void shouldDescribeRenamedSystemRoutes() {
        assertEquals("查询审计日志",
                AuditLogText.description(LogOT.READ, "DATA", "/api/v1/audit-log/page/1/15"));
        assertEquals("导出数据集",
                AuditLogText.description(LogOT.EXPORT, "DATASET", "/api/v1/dataset-tree/export-dataset"));
        assertEquals("维护系统参数",
                AuditLogText.description(LogOT.MODIFY, "DATA", "/api/v1/sys-parameter/basic/record"));
    }
}
