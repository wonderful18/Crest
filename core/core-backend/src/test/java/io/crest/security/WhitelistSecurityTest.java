package io.crest.security;

import io.crest.utils.WhitelistUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全测试：验证敏感端点已从白名单移除
 *
 * 修复漏洞：PT-01（/symmetric-key 密钥泄露）
 * 注意：/public-key 保留在白名单中，因为登录流程需要 RSA 公钥
 */
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
    @DisplayName("健康检查端点应在白名单中")
    void healthShouldBeInWhitelist() {
        assertTrue(WhitelistUtils.WHITE_PATH.contains("/actuator/health"),
                "健康检查端点应在白名单中");
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
