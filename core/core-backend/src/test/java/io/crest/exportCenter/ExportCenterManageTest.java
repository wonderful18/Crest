package io.crest.exportCenter;

import io.crest.api.chart.request.ChartExcelRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.auth.config.SubstituleLoginConfig;
import io.crest.exportCenter.dao.auto.entity.CoreExportDownloadTask;
import io.crest.exportCenter.dao.auto.entity.CoreExportTask;
import io.crest.exportCenter.dao.auto.mapper.CoreExportDownloadTaskMapper;
import io.crest.exportCenter.dao.auto.mapper.CoreExportTaskMapper;
import io.crest.exportCenter.manage.ExportCenterDownLoadManage;
import io.crest.exportCenter.manage.ExportCenterManage;
import io.crest.utils.AuthUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
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
    private CoreExportDownloadTaskMapper coreExportDownloadTaskMapper;

    @Mock
    private ExportCenterDownLoadManage exportCenterDownLoadManage;

    @TempDir
    private Path exportRoot;

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "ready", false);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "pwd", null);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "tokenSecret", null);
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

    @Test
    @DisplayName("下载票据应允许导出中心刷新下载任务时间")
    void downloadTicketShouldSurviveDownloadTaskTimeRefresh() throws IOException {
        ExportCenterManage manage = new ExportCenterManage();
        ReflectionTestUtils.setField(manage, "exportTaskMapper", exportTaskMapper);
        ReflectionTestUtils.setField(manage, "coreExportDownloadTaskMapper", coreExportDownloadTaskMapper);
        ReflectionTestUtils.setField(manage, "exportCenterDownLoadManage", exportCenterDownLoadManage);
        ReflectionTestUtils.setField(manage, "exportData_path", exportRoot.toString());

        CoreExportTask task = new CoreExportTask();
        task.setId("task-1");
        task.setUserId(1L);
        task.setExportStatus("SUCCESS");
        task.setExportTime(System.currentTimeMillis());
        task.setFileName("export.xlsx");

        Path taskDir = exportRoot.resolve("task-1");
        Files.createDirectories(taskDir);
        Files.writeString(taskDir.resolve("task-1.xlsx"), "xlsx");

        long ticketTime = System.currentTimeMillis();
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "ready", true);
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "pwd", "admin");
        ReflectionTestUtils.setField(SubstituleLoginConfig.class, "tokenSecret", "test-secret");
        String ticket = ReflectionTestUtils.invokeMethod(manage, "buildDownloadTicket", task, ticketTime, 5L);

        CoreExportDownloadTask downloadTask = new CoreExportDownloadTask();
        downloadTask.setId("task-1");
        downloadTask.setCreateTime(ticketTime + 1000);
        downloadTask.setValidTime(5L);

        when(exportTaskMapper.selectById("task-1")).thenReturn(task);
        when(coreExportDownloadTaskMapper.selectById("task-1")).thenReturn(downloadTask);

        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        manage.download("task-1", ticket, response);

        verify(exportCenterDownLoadManage).download(same(task), same(response));
        verify(coreExportDownloadTaskMapper).deleteById("task-1");
    }
}
