package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.UserCreator;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.utils.PasswordEncoder;
import io.crest.utils.PasswordValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CrestUserManageTest {

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
}
