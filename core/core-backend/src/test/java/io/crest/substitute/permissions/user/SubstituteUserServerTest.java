package io.crest.substitute.permissions.user;

import io.crest.api.permissions.user.dto.ModifyPwdRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
