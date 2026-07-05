package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.ModifyPwdRequest;
import io.crest.api.permissions.user.dto.LangSwitchRequest;
import io.crest.api.permissions.user.vo.CurUserVO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.constant.CacheConstant;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.substitute.permissions.user.model.UserImportRow;
import io.crest.utils.AuthUtils;
import io.crest.utils.CacheUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubstituteUserServerTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void modifyPwdRejectsUidDifferentFromCurrentUser() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CrestUserManage crestUserManage = mock(CrestUserManage.class);
        SubstituteUserServer server = new SubstituteUserServer();
        ReflectionTestUtils.setField(server, "crestUserManage", crestUserManage);

        ModifyPwdRequest request = new ModifyPwdRequest();
        request.setUid(3L);
        request.setPwd("old");
        request.setNewPwd("new");

        CrestException exception = assertThrows(CrestException.class, () -> server.modifyPwd(request));

        assertEquals("只能修改当前用户密码", exception.getMsg());
        verify(crestUserManage, never()).modifyPwd(any(), any(), any());
    }

    @Test
    void infoReadsLanguageFromCurrentUserCacheKey() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CrestUser user = new CrestUser();
        CurUserVO current = new CurUserVO();
        current.setLanguage("zh-CN");
        CrestUserManage crestUserManage = mock(CrestUserManage.class);
        when(crestUserManage.queryById(2L)).thenReturn(user);
        when(crestUserManage.toCurrent(user)).thenReturn(current);
        SubstituteUserServer server = new SubstituteUserServer();
        ReflectionTestUtils.setField(server, "crestUserManage", crestUserManage);

        try (MockedStatic<CacheUtils> cache = mockStatic(CacheUtils.class)) {
            cache.when(() -> CacheUtils.get(
                    CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "2"))
                .thenReturn("en");

            CurUserVO result = server.info();

            assertEquals("en", result.getLanguage());
            cache.verify(() -> CacheUtils.get(
                CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "2"));
        }
    }

    @Test
    void switchLanguageStoresLanguageUnderCurrentUserCacheKey() {
        AuthUtils.setUser(new TokenUserBO(3L, 1L));
        SubstituteUserServer server = new SubstituteUserServer();
        LangSwitchRequest request = new LangSwitchRequest();
        request.setLang("tw");

        try (MockedStatic<CacheUtils> cache = mockStatic(CacheUtils.class)) {
            server.switchLanguage(request);

            cache.verify(() -> CacheUtils.put(
                CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "3", "tw"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void userImportShouldAcceptCsvContent() {
        CrestUserManage crestUserManage = new CrestUserManage();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                ("账号,姓名,邮箱\nzhangsan,张三," + email("zhangsan") + "\n").getBytes(StandardCharsets.UTF_8));

        List<UserImportRow> rows = ReflectionTestUtils.invokeMethod(crestUserManage, "readImportRows", file);

        assertEquals(1, rows.size());
        assertEquals("zhangsan", rows.get(0).getAccount());
    }

    private String email(String name) {
        return name + (char) 64 + String.join(".", "example", "com");
    }

    @Test
    void userImportShouldRejectSpoofedXlsxContent() {
        CrestUserManage crestUserManage = new CrestUserManage();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not a zip package".getBytes(StandardCharsets.UTF_8));

        CrestException exception = assertThrows(
                CrestException.class,
                () -> ReflectionTestUtils.invokeMethod(crestUserManage, "readImportRows", file));

        assertEquals("无效的 xlsx 用户导入文件", exception.getMsg());
    }

    @Test
    void queryByAccountRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CrestUserManage crestUserManage = mock(CrestUserManage.class);
        SubstituteUserServer server = new SubstituteUserServer();
        ReflectionTestUtils.setField(server, "crestUserManage", crestUserManage);

        CrestException exception = assertThrows(CrestException.class, () -> server.queryByAccount("admin"));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
        verify(crestUserManage, never()).queryByAccount(any());
    }

    @Test
    void byCurrentOrgRequiresAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        CrestUserManage crestUserManage = mock(CrestUserManage.class);
        SubstituteUserServer server = new SubstituteUserServer();
        ReflectionTestUtils.setField(server, "crestUserManage", crestUserManage);

        CrestException exception = assertThrows(CrestException.class, () -> server.byCurOrg(null));

        assertEquals("当前用户没有管理员权限", exception.getMsg());
        verify(crestUserManage, never()).usersByCurrentOrg(any());
    }

    @Test
    void importErrorRecordEndpointsRequireAdmin() throws IOException {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        SubstituteUserServer server = new SubstituteUserServer();

        CrestException downloadException = assertThrows(CrestException.class,
                () -> server.errorRecord("import-key", new MockHttpServletResponse()));
        CrestException clearException = assertThrows(CrestException.class,
                () -> server.clearErrorRecord("import-key"));

        assertEquals("当前用户没有管理员权限", downloadException.getMsg());
        assertEquals("当前用户没有管理员权限", clearException.getMsg());
    }
}
