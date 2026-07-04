package io.crest.substitute.permissions.user;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrestUserManageSecurityTest {

    @Test
    void importUsersRejectsUnsupportedFileSuffixBeforeParsingRows() {
        CrestUserManage manage = new CrestUserManage();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "users.txt",
                "text/plain",
                "account,name\nalice,Alice\n".getBytes());

        CrestException exception = assertThrows(CrestException.class, () -> manage.importUsers(file));

        assertEquals("仅支持 xlsx、xls、csv 格式的用户导入文件", exception.getMsg());
    }
}
