package io.crest.system.manage;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.crest.auth.vo.TokenVO;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.substitute.permissions.user.model.CrestUser;
import io.crest.utils.SignedTokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SsoManageTokenTest {

    @Test
    void generatedSsoTokenExpiresAfterOneDay() {
        SsoManage manage = new SsoManage();
        PlatformPermissionManage platformPermissionManage = mock(PlatformPermissionManage.class);
        when(platformPermissionManage.defaultOrgId(12L)).thenReturn(1L);
        ReflectionTestUtils.setField(manage, "platformPermissionManage", platformPermissionManage);

        CrestUser user = new CrestUser();
        user.setId(12L);
        user.setPasswordHash("user-secret");

        TokenVO token = ReflectionTestUtils.invokeMethod(manage, "generate", user);

        assertNotNull(token);
        assertEquals(24 * 60 * 60 * 1000L, token.getExp());
        DecodedJWT jwt = SignedTokenUtils.verify(token.getToken(), "user-secret");
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().after(new Date()));
    }
}
