package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.UserCreator;
import io.crest.api.permissions.user.dto.UserEditor;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.utils.PasswordEncoder;
import io.crest.utils.PasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrestUserManageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CrestUserManage crestUserManage;

    @Test
    public void createRejectsHtmlLikeDisplayName() {
        ReflectionTestUtils.setField(crestUserManage, "configuredInitialPassword", "TestInitialPassword123!");

        UserCreator creator = new UserCreator();
        creator.setAccount("safe.user");
        creator.setName("<script>alert(1)</script>");
        creator.setRoleIds(List.of(2L));
        creator.setEnable(true);

        assertThrows(CrestException.class, () -> crestUserManage.create(creator));
    }

    @Test
    public void resetPwdUsesOneTimeTemporaryPassword() {
        CrestUser user = new CrestUser();
        user.setId(30L);
        user.setAuthType(CrestUserManage.AUTH_TYPE_LOCAL);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<CrestUser>>any(), eq(30L)))
                .thenReturn(List.of(user));

        String temporaryPassword = crestUserManage.resetPwd(30L);

        assertNotEquals("EnvInitialPassword123!", temporaryPassword);
        PasswordValidator.validate(temporaryPassword);
        verify(jdbcTemplate).update(anyString(),
                argThat(hash -> hash instanceof String value && PasswordEncoder.matches(temporaryPassword, value)),
                anyLong(),
                eq(30L));
    }

    @Test
    public void editRejectsLocalPasswordForSsoUser() {
        CrestUser user = new CrestUser();
        user.setId(40L);
        user.setAccount("sso.user");
        user.setName("SSO 用户");
        user.setAuthType(CrestUserManage.AUTH_TYPE_SSO);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<CrestUser>>any(), eq(40L)))
                .thenReturn(List.of(user));

        UserEditor editor = new UserEditor();
        editor.setId(40L);
        editor.setAccount("sso.user");
        editor.setName("SSO 用户");
        editor.setRoleIds(List.of(2L));
        editor.setPassword("StrongPassword123!");

        assertThrows(CrestException.class, () -> crestUserManage.edit(editor));
    }

    @Test
    public void initialPasswordRequiresConfiguration() {
        ReflectionTestUtils.setField(crestUserManage, "configuredInitialPassword", "");

        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(crestUserManage, "initialPassword"));
    }
}
