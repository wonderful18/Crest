package io.crest.exportCenter;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.crest.exportCenter.manage.ExportCenterManage;
import io.crest.exportCenter.server.ExportCenterServer;
import io.crest.model.ExportTaskDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportCenterServerTest {

    @Test
    void pagerShouldDefaultLegacyUndefinedPathParameters() {
        ExportCenterManage manage = mock(ExportCenterManage.class);
        ExportCenterServer server = new ExportCenterServer();
        ReflectionTestUtils.setField(server, "exportCenterManage", manage);
        Page<ExportTaskDTO> expected = new Page<>(1, 10);
        when(manage.pager(any(), eq("ALL"))).thenReturn(expected);

        IPage<ExportTaskDTO> result = server.pager("undefined", "undefined", "undefined");

        assertSame(expected, result);
        ArgumentCaptor<Page<ExportTaskDTO>> pageCaptor = pageCaptor();
        verify(manage).pager(pageCaptor.capture(), eq("ALL"));
        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());
        assertEquals(false, pageCaptor.getValue().optimizeCountSql());
    }

    @Test
    void pagerShouldKeepValidParameters() {
        ExportCenterManage manage = mock(ExportCenterManage.class);
        ExportCenterServer server = new ExportCenterServer();
        ReflectionTestUtils.setField(server, "exportCenterManage", manage);
        Page<ExportTaskDTO> expected = new Page<>(2, 20);
        when(manage.pager(any(), eq("FAILED"))).thenReturn(expected);

        IPage<ExportTaskDTO> result = server.pager("2", "20", "failed");

        assertSame(expected, result);
        ArgumentCaptor<Page<ExportTaskDTO>> pageCaptor = pageCaptor();
        verify(manage).pager(pageCaptor.capture(), eq("FAILED"));
        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(20, pageCaptor.getValue().getSize());
        assertEquals(false, pageCaptor.getValue().optimizeCountSql());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Page<ExportTaskDTO>> pageCaptor() {
        return ArgumentCaptor.forClass((Class) Page.class);
    }
}
