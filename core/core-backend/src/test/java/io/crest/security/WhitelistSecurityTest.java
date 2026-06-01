package io.crest.security;

import io.crest.utils.WhitelistUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhitelistSecurityTest {

    @Test
    @DisplayName("敏感端点 /symmetric-key 不应在白名单中")
    void symmetricKeyShouldNotBeInWhitelist() {
        assertFalse(WhitelistUtils.WHITE_PATH.contains("/symmetric-key"),
                "/symmetric-key 端点仍在白名单中，会导致 AES 密钥泄露");
    }

    @Test
    @DisplayName("/public-key 应在白名单中（登录流程需要）")
    void publicKeyShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.WHITE_PATH.contains("/public-key"),
                "/public-key 端点不在白名单中，登录流程将无法工作");
    }

    @Test
    @DisplayName("登录端点应在白名单中")
    void loginShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.WHITE_PATH.contains("/login/local-login"),
                "登录端点应在白名单中");
    }

    @Test
    @DisplayName("登录页启动配置端点应在白名单中")
    void loginBootstrapEndpointsShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.match("/api/v1/sys-parameter/default-settings"));
        assertTrue(WhitelistUtils.match("/api/v1/sys-parameter/ui"));
        assertTrue(WhitelistUtils.match("/api/v1/typeface/default"));
        assertTrue(WhitelistUtils.match("/api/v1/typeface/fonts"));
    }

    @Test
    @DisplayName("分享链接启动接口应允许无登录态访问")
    void shareBootstrapEndpointsShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.match("/api/v1/share/proxy-info"));
        assertTrue(WhitelistUtils.match("/api/v1/share/validate"));
        assertFalse(WhitelistUtils.match("/api/v1/share/proxyInfo"),
                "不存在的历史路径不应继续保留白名单");
        assertFalse(WhitelistUtils.match("/api/v1/share/list"),
                "分享管理列表必须保留登录态");
    }

    @Test
    @DisplayName("健康检查端点应在白名单中")
    void healthShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.WHITE_PATH.contains("/actuator/health"),
                "健康检查端点应在白名单中");
        assertTrue(WhitelistUtils.match("/api/v1/actuator/health/liveness"),
                "存活检查端点应在白名单中");
        assertTrue(WhitelistUtils.match("/api/v1/actuator/health/readiness"),
                "就绪检查端点应在白名单中");
    }

    @Test
    @DisplayName("Prometheus 指标端点不应进入普通白名单")
    void prometheusEndpointShouldNotBeInWhitelist() {
        assertFalse(WhitelistUtils.WHITE_PATH.contains("/actuator/prometheus"),
                "指标端点必须使用专用抓取认证，不能进入普通白名单");
        assertFalse(WhitelistUtils.match("/api/v1/actuator/prometheus"),
                "指标端点必须使用专用抓取认证，不能被普通白名单放行");
    }

    @Test
    @DisplayName("导出中心下载地址应通过签名链接访问")
    void exportCenterDownloadShouldBeWhitelisted() {
        assertTrue(WhitelistUtils.match("/api/v1/export-center/download/task-id"),
                "导出中心下载地址使用签名 ticket 校验，应允许浏览器直接打开");
        assertFalse(WhitelistUtils.match("/api/v1/export-center/download-tickets/task-id"),
                "生成下载地址必须保留登录态，不能被下载白名单误放行");
    }

    @Test
    @DisplayName("/symmetric-key 不应在白名单中（通过match方法验证）")
    void symmetricKeyShouldNotMatch() {
        assertFalse(WhitelistUtils.match("/api/v1/symmetric-key"),
                "/symmetric-key 应该需要认证");
    }
}
