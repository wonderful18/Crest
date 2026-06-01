package io.crest.exportCenter;

import io.crest.api.chart.request.ChartExcelRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exportCenter.dao.auto.entity.CoreExportTask;
import io.crest.exportCenter.dao.auto.mapper.CoreExportTaskMapper;
import io.crest.exportCenter.manage.ExportCenterDownLoadManage;
import io.crest.exportCenter.manage.ExportCenterManage;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportCenterManageTest {

    @Mock
    private CoreExportTaskMapper exportTaskMapper;

    @Mock
    private ExportCenterDownLoadManage exportCenterDownLoadManage;

    @TempDir
    private Path exportRoot;

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    @DisplayName("重试导出任务时应清理旧失败信息")
    void retryShouldClearStaleFailureMessage() {
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        ExportCenterManage manage = new ExportCenterManage();
        ReflectionTestUtils.setField(manage, "exportTaskMapper", exportTaskMapper);
        ReflectionTestUtils.setField(manage, "exportCenterDownLoadManage", exportCenterDownLoadManage);
        ReflectionTestUtils.setField(manage, "exportData_path", exportRoot.toString());

        CoreExportTask task = new CoreExportTask();
        task.setId("task-1");
        task.setUserId(1L);
        task.setExportFrom(980107L);
        task.setExportFromType("chart");
        task.setExportStatus("FAILED");
        task.setExportProgress("100");
        task.setMsg("导出文件不存在或已被清理，请重新导出");
        task.setFileSize(9.78);
        task.setFileSizeUnit("Kb");
        task.setParams("{}");

        when(exportTaskMapper.selectById("task-1")).thenReturn(task);
        when(exportTaskMapper.updateById(any(CoreExportTask.class))).thenReturn(1);

        manage.retry("task-1");

        assertEquals("PENDING", task.getExportStatus());
        assertEquals("0", task.getExportProgress());
        assertNull(task.getMsg());
        assertNull(task.getFileSize());
        assertNull(task.getFileSizeUnit());
        verify(exportCenterDownLoadManage).startViewTask(same(task), any(ChartExcelRequest.class));
    }
}
