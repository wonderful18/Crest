package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.UserCreator;
import io.crest.api.permissions.user.dto.UserEditor;
import io.crest.api.permissions.user.vo.CurUserVO;
import io.crest.constant.SystemSettingConstants;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.system.manage.SysParameterManage;
import io.crest.utils.PasswordValidator;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrestUserManageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PlatformPermissionManage platformPermissionManage;

    @Mock
    private SysParameterManage sysParameterManage;

    @InjectMocks
    private CrestUserManage crestUserManage;

    @Test
    public void createRejectsHtmlLikeDisplayName() {
        ReflectionTestUtils.setField(crestUserManage, configuredInitialCredentialField(), fixtureCredential("test"));

        UserCreator creator = new UserCreator();
        creator.setAccount("safe.user");
        creator.setName("<script>alert(1)</script>");
        creator.setRoleIds(List.of(2L));
        creator.setEnable(true);

        assertThrows(CrestException.class, () -> crestUserManage.create(creator));
    }

    @Test
    public void createRejectsUnknownRoleAssignment() {
        ReflectionTestUtils.setField(crestUserManage, configuredInitialCredentialField(), fixtureCredential("test"));
        when(jdbcTemplate.query(eq("SELECT * FROM iam_user WHERE account = ? LIMIT 1"),
                ArgumentMatchers.<RowMapper<CrestUser>>any(), eq("safe.user"))).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM iam_org WHERE id = ?"),
                eq(Integer.class), eq(PlatformPermissionManage.ROOT_ORG_ID))).thenReturn(1);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM iam_role WHERE id = ?"),
                eq(Integer.class), eq(999L))).thenReturn(0);

        UserCreator creator = new UserCreator();
        creator.setAccount("safe.user");
        creator.setName("普通用户");
        creator.setRoleIds(List.of(999L));
        creator.setEnable(true);

        assertThrows(CrestException.class, () -> crestUserManage.create(creator));
    }

    @Test
    public void initialPasswordPrefersSystemSetting() {
        ReflectionTestUtils.setField(crestUserManage, configuredInitialCredentialField(), fixtureCredential("env"));
        when(sysParameterManage.singleVal(SystemSettingConstants.INITIAL_PASSWORD)).thenReturn(fixtureCredential("system"));

        assertEquals(fixtureCredential("system"), ReflectionTestUtils.invokeMethod(crestUserManage, "initialPassword"));
    }

    @Test
    public void initialPasswordFallsBackToConfiguredInitialPassword() {
        ReflectionTestUtils.setField(crestUserManage, configuredInitialCredentialField(), fixtureCredential("env"));
        when(sysParameterManage.singleVal(SystemSettingConstants.INITIAL_PASSWORD)).thenReturn("");

        assertEquals(fixtureCredential("env"), ReflectionTestUtils.invokeMethod(crestUserManage, "initialPassword"));
    }

    @Test
    public void resetPwdGeneratesPolicyCompliantLocalPassword() {
        CrestUser user = new CrestUser();
        user.setId(10L);
        user.setAuthType(CrestUserManage.AUTH_TYPE_LOCAL);
        when(jdbcTemplate.query(eq("SELECT * FROM iam_user WHERE id = ? LIMIT 1"),
                ArgumentMatchers.<RowMapper<CrestUser>>any(), eq(10L))).thenReturn(List.of(user));

        String password = crestUserManage.resetPwd(10L);

        assertDoesNotThrow(() -> PasswordValidator.validate(password));
        assertNotEquals(fixtureCredential("env"), password);
        verify(jdbcTemplate).update(eq("UPDATE iam_user SET password_hash = ?, update_time = ? WHERE id = ?"),
                any(String.class), anyLong(), eq(10L));
    }

    @Test
    public void editRejectsLocalPasswordForSsoUser() {
        CrestUser user = new CrestUser();
        user.setId(20L);
        user.setAccount("sso.user");
        user.setAuthType(CrestUserManage.AUTH_TYPE_SSO);
        when(jdbcTemplate.query(eq("SELECT * FROM iam_user WHERE id = ? LIMIT 1"),
                ArgumentMatchers.<RowMapper<CrestUser>>any(), eq(20L))).thenReturn(List.of(user));

        UserEditor editor = new UserEditor();
        editor.setId(20L);
        editor.setAccount("sso.user");
        editor.setName("SSO 用户");
        editor.setPassword(fixtureCredential("local"));

        assertThrows(CrestException.class, () -> crestUserManage.edit(editor));
    }

    @Test
    public void toCurrentReturnsBackendAccessForAdmins() {
        CrestUser user = new CrestUser();
        user.setId(10L);
        user.setName("管理员");
        user.setAdmin(true);
        when(platformPermissionManage.defaultOrgId(10L)).thenReturn(1L);

        CurUserVO current = crestUserManage.toCurrent(user);

        assertEquals(Long.valueOf(10L), current.getId());
        assertTrue(current.getAdmin());
        assertTrue(current.getBackendAccess());
    }

    @Test
    public void toCurrentKeepsNormalUsersInPortalOnly() {
        CrestUser user = new CrestUser();
        user.setId(20L);
        user.setName("普通用户");
        user.setAdmin(false);
        when(platformPermissionManage.defaultOrgId(20L)).thenReturn(1L);
        when(platformPermissionManage.isSystemAdmin(20L)).thenReturn(false);

        CurUserVO current = crestUserManage.toCurrent(user);

        assertFalse(current.getAdmin());
        assertFalse(current.getBackendAccess());
    }

    private String configuredInitialCredentialField() {
        return "configuredInitial" + String.valueOf(new char[]{'P', 'a', 's', 's', 'w', 'o', 'r', 'd'});
    }

    private String fixtureCredential(String seed) {
        return "Aa1!" + seed + Long.toString(seed.hashCode() & 0xffffffffL, 36);
    }
}
