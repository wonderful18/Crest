package io.crest.exportCenter;

import io.crest.exception.CrestException;
import io.crest.exportCenter.dao.auto.entity.CoreExportTask;
import io.crest.exportCenter.manage.ExportCenterDownLoadManage;
import io.crest.i18n.Translator;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ExportCenterDownloadManageTest {

    @Test
    @DisplayName("导出文件不存在时应返回明确提示")
    void missingExportFileShouldReturnClearMessage() throws Exception {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("i18n_export_file_missing", Locale.SIMPLIFIED_CHINESE,
                "导出文件不存在或已被清理，请重新导出");
        new Translator().setMessageSource(messageSource);
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);

        ExportCenterDownLoadManage manage = new ExportCenterDownLoadManage();
        Path exportRoot = Files.createTempDirectory("crest-export-missing-");
        ReflectionTestUtils.setField(manage, "exportData_path", exportRoot.toString());

        CoreExportTask task = new CoreExportTask();
        task.setId("missing-task");
        task.setFileName("missing.xlsx");
        task.setExportTime(System.currentTimeMillis());

        CrestException exception = assertThrows(CrestException.class,
                () -> manage.download(task, mock(HttpServletResponse.class)));

        assertEquals("导出文件不存在或已被清理，请重新导出", exception.getMsg());
    }
}
