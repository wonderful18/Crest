package io.crest.share.manage;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShareLinkAccessManageTest {

    @Test
    void allowsOriginalShareResourceWithoutQueryingJumpConfig() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShareLinkAccessManage manage = new ShareLinkAccessManage(jdbcTemplate);

        assertTrue(manage.canAccessWithShareResource(100L, 100L));

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void allowsConfiguredInnerJumpTarget() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(100L), eq(200L), eq(100L), eq(200L)))
                .thenReturn(1L);
        ShareLinkAccessManage manage = new ShareLinkAccessManage(jdbcTemplate);

        assertTrue(manage.canAccessWithShareResource(100L, 200L));

        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq(100L), eq(200L), eq(100L), eq(200L));
    }

    @Test
    void rejectsUnconfiguredTarget() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(100L), eq(300L), eq(100L), eq(300L)))
                .thenReturn(0L);
        ShareLinkAccessManage manage = new ShareLinkAccessManage(jdbcTemplate);

        assertFalse(manage.canAccessWithShareResource(100L, 300L));
    }

    @Test
    void rejectsMissingIds() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShareLinkAccessManage manage = new ShareLinkAccessManage(jdbcTemplate);

        assertFalse(manage.canAccessWithShareResource(null, 300L));
        assertFalse(manage.canAccessWithShareResource(100L, null));

        verifyNoInteractions(jdbcTemplate);
    }
}
