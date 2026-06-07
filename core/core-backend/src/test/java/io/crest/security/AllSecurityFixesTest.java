package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("安全基线综合测试")
class AllSecurityFixesTest {

    @Nested
    @DisplayName("白名单安全测试")
    class WhitelistSecurityTests {

        @Test
        @DisplayName("敏感端点 /symmetric-key 不应在白名单中")
        void symmetricKeyNotInWhitelist() {
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
        void loginInWhitelist() {
            assertTrue(WhitelistUtils.WHITE_PATH.contains("/login/local-login"));
        }

        @Test
        @DisplayName("API 文档入口不应匿名访问")
        void apiDocsNotInWhitelist() {
            assertFalse(WhitelistUtils.WHITE_PATH.contains("/doc.html"));
            assertFalse(WhitelistUtils.WHITE_PATH.contains("/swagger-ui.html"));
            assertFalse(WhitelistUtils.WHITE_PATH.contains("/swagger-resources"));
            assertFalse(WhitelistUtils.match("/api/v1/doc.html"));
            assertFalse(WhitelistUtils.match("/api/v1/swagger-ui/index.html"));
            assertFalse(WhitelistUtils.match("/api/v1/webjars/swagger-ui/index.html"));
        }

        @Test
        @DisplayName("空用户 ID 不应被识别为系统管理员")
        void nullUserIdShouldNotBeSystemAdmin() {
            assertFalse(AuthUtils.isSysAdmin(null));
        }
    }

    @Nested
    @DisplayName("SSRF 防护测试")
    class SsrfProtectionTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "http://127.0.0.1/",
                "http://127.1/",
                "http://2130706433/",
                "http://[::1]/",
                "http://localhost/",
                "http://0.0.0.0/",
                "http://169.254.169.254/",
                "http://10.0.0.1/",
                "http://172.16.0.1/",
                "http://192.168.1.1/",
                "file:///etc/passwd",
                "gopher://127.0.0.1/"
        })
        @DisplayName("应阻止内部地址和危险协议")
        void shouldBlockInternalAddresses(String url) {
            assertThrows(CrestException.class, () -> SsrfProtection.validateUrl(url));
        }

        @Test
        @DisplayName("应允许外部 HTTP 地址")
        void shouldAllowExternalHttp() {
            assertDoesNotThrow(() -> SsrfProtection.validateUrl("http://93.184.216.34/"));
        }

        @Test
        @DisplayName("应允许外部 HTTPS 地址")
        void shouldAllowExternalHttps() {
            assertDoesNotThrow(() -> SsrfProtection.validateUrl("https://93.184.216.34/"));
        }

        @Test
        @DisplayName("应阻止数据库端口")
        void shouldBlockDatabasePorts() {
            assertThrows(CrestException.class, () -> SsrfProtection.validateUrl("http://example.com:3306/"));
            assertThrows(CrestException.class, () -> SsrfProtection.validateUrl("http://example.com:5432/"));
            assertThrows(CrestException.class, () -> SsrfProtection.validateUrl("http://example.com:6379/"));
        }
    }

    @Nested
    @DisplayName("XSS 防护测试")
    class XssProtectionTests {

        @Test
        @DisplayName("应移除 script 标签")
        void shouldRemoveScriptTags() {
            String input = "<script>alert(1)</script>Hello";
            String result = XssProtection.sanitize(input);
            assertFalse(result.contains("<script>"));
            assertTrue(result.contains("Hello"));
        }

        @Test
        @DisplayName("应移除事件处理器")
        void shouldRemoveEventHandlers() {
            String input = "<img src=x onerror=alert(1)>";
            String result = XssProtection.sanitize(input);
            assertFalse(result.toLowerCase().contains("onerror"));
        }

        @Test
        @DisplayName("应移除 javascript: 协议")
        void shouldRemoveJavascriptProtocol() {
            String input = "<a href=\"javascript:alert(1)\">Click</a>";
            String result = XssProtection.sanitize(input);
            assertFalse(result.toLowerCase().contains("javascript:"));
        }

        @Test
        @DisplayName("应转义 HTML 实体")
        void shouldEncodeHtmlEntities() {
            String input = "<div>Test & \"quotes\"</div>";
            String result = XssProtection.encodeForHtml(input);
            assertTrue(result.contains("&lt;"));
            assertTrue(result.contains("&gt;"));
            assertTrue(result.contains("&amp;"));
        }

        @Test
        @DisplayName("正常文本应保持不变")
        void shouldPreserveNormalText() {
            String input = "Hello World 123";
            String result = XssProtection.sanitize(input);
            assertTrue(result.contains("Hello World 123"));
        }

        @Test
        @DisplayName("应检测 XSS 内容")
        void shouldDetectXssContent() {
            assertTrue(XssProtection.containsXss("<script>alert(1)</script>"));
            assertTrue(XssProtection.containsXss("onerror=alert(1)"));
            assertFalse(XssProtection.containsXss("Hello World"));
        }
    }

    @Nested
    @DisplayName("密码编码器测试")
    class PasswordEncoderTests {

        @Test
        @DisplayName("应正确编码密码")
        void shouldEncodePassword() {
            String encoded = PasswordEncoder.encode("TestPassword123!");
            assertNotNull(encoded);
            assertTrue(encoded.contains(":"));
        }

        @Test
        @DisplayName("相同密码应产生不同的哈希")
        void shouldProduceDifferentHashes() {
            String encoded1 = PasswordEncoder.encode("TestPassword123!");
            String encoded2 = PasswordEncoder.encode("TestPassword123!");
            assertNotEquals(encoded1, encoded2);
        }

        @Test
        @DisplayName("应正确验证密码")
        void shouldVerifyPassword() {
            String password = "TestPassword123!";
            String encoded = PasswordEncoder.encode(password);
            assertTrue(PasswordEncoder.matches(password, encoded));
        }

        @Test
        @DisplayName("错误密码应验证失败")
        void shouldRejectWrongPassword() {
            String encoded = PasswordEncoder.encode("TestPassword123!");
            assertFalse(PasswordEncoder.matches("WrongPassword", encoded));
        }

        @Test
        @DisplayName("应检测 MD5 格式需要升级")
        void shouldDetectMd5NeedsUpgrade() {
            String md5Hash = "e10adc3949ba59abbe56e057f20f883e";
            assertTrue(PasswordEncoder.needsReEncoding(md5Hash));
        }

        @Test
        @DisplayName("应检测新格式不需要升级")
        void shouldDetectNewFormatNoUpgrade() {
            String encoded = PasswordEncoder.encode("TestPassword123!");
            assertFalse(PasswordEncoder.needsReEncoding(encoded));
        }
    }

    @Nested
    @DisplayName("密码策略验证器测试")
    class PasswordValidatorTests {

        @Test
        @DisplayName("强密码应通过验证")
        void shouldAcceptStrongPassword() {
            assertDoesNotThrow(() -> PasswordValidator.validate("MyStr0ng!Pass"));
            assertDoesNotThrow(() -> PasswordValidator.validate("C0mplex@Password"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "123",
                "1234567",
                "password",
                "Pass1!",
                "short"
        })
        @DisplayName("弱密码应被拒绝")
        void shouldRejectWeakPasswords(String password) {
            assertThrows(CrestException.class, () -> PasswordValidator.validate(password));
        }

        @Test
        @DisplayName("缺少大写字母应被拒绝")
        void shouldRejectWithoutUppercase() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate("mypassword1!"));
        }

        @Test
        @DisplayName("缺少小写字母应被拒绝")
        void shouldRejectWithoutLowercase() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate("MYPASSWORD1!"));
        }

        @Test
        @DisplayName("缺少数字应被拒绝")
        void shouldRejectWithoutDigit() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate("MyPassword!"));
        }

        @Test
        @DisplayName("缺少特殊字符应被拒绝")
        void shouldRejectWithoutSpecialChar() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate("MyPassword1"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "12345678",
                "password",
                "admin123"
        })
        @DisplayName("常见弱密码应被拒绝")
        void shouldRejectCommonPasswords(String password) {
            assertThrows(CrestException.class, () -> PasswordValidator.validate(password));
        }

        @Test
        @DisplayName("连续相同字符应被拒绝")
        void shouldRejectSequentialChars() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate("Aaa111!@#"));
        }

        @Test
        @DisplayName("null 密码应抛出异常")
        void shouldRejectNull() {
            assertThrows(CrestException.class, () -> PasswordValidator.validate(null));
        }

        @Test
        @DisplayName("应返回策略描述")
        void shouldReturnPolicyDescription() {
            String description = PasswordValidator.getPolicyDescription();
            assertNotNull(description);
            assertTrue(description.contains("8"));
        }
    }
}
