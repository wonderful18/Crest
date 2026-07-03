package io.crest.substitute.permissions.user;

import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.system.manage.SysParameterManage;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrestUserManageInitAdminTest {

    @Test
    void shouldNormalizeAdminWhenAnotherPodCreatedItFirst() {
        CrestUserManage manage = new CrestUserManage();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SysParameterManage sysParameterManage = mock(SysParameterManage.class);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM core_iam_user WHERE id = 1"), eq(Integer.class)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(eq("SELECT password_hash FROM core_iam_user WHERE id = 1"), eq(String.class)))
                .thenReturn("encoded-password");
        doThrow(new DuplicateKeyException("duplicate")).when(jdbcTemplate).update(
                startsWith("INSERT INTO core_iam_user"),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
        when(sysParameterManage.singleVal(any())).thenReturn("");

        ReflectionTestUtils.setField(manage, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(manage, "environment", new MockEnvironment());
        ReflectionTestUtils.setField(manage, "platformPermissionManage", mock(PlatformPermissionManage.class));
        ReflectionTestUtils.setField(manage, "sysParameterManage", sysParameterManage);
        ReflectionTestUtils.setField(manage, "configuredInitialPassword", "InitialPassword123!");

        manage.initAdmin();

        verify(jdbcTemplate).update("UPDATE core_iam_user SET is_admin = 1, enable = 1 WHERE id = 1");
    }
}
