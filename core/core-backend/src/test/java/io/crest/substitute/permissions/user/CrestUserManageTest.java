package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.UserCreator;
import io.crest.api.permissions.user.vo.CurUserVO;
import io.crest.exception.CrestException;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CrestUserManageTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PlatformPermissionManage platformPermissionManage;

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
}
