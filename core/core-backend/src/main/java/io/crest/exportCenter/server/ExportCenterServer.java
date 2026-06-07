package io.crest.exportCenter.server;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.crest.api.exportCenter.ExportCenterApi;
import io.crest.constant.LogOT;
import io.crest.constant.LogST;
import io.crest.exception.CrestException;
import io.crest.exportCenter.manage.ExportCenterManage;
import io.crest.exportCenter.util.ExportCenterUtils;
import io.crest.log.CrestAudit;
import io.crest.model.ExportTaskDTO;
import io.crest.result.ResultMessage;
import io.crest.utils.JsonUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/export-center")
@Transactional(rollbackFor = Exception.class)
public class ExportCenterServer implements ExportCenterApi {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_STATUS = "ALL";
    private static final Set<String> EXPORT_STATUSES = Set.of("SUCCESS", "FAILED", "PENDING", "IN_PROGRESS", "ALL");

    @Resource
    private ExportCenterManage exportCenterManage;

    @Override
    public Map<String, Long> exportTasks() {
        return exportCenterManage.exportTasks();
    }

    @Override
    public IPage<ExportTaskDTO> pager(String goPage, String pageSize, String status) {
        Page<ExportTaskDTO> page = new Page<>(
                positiveIntOrDefault(goPage, DEFAULT_PAGE),
                positiveIntOrDefault(pageSize, DEFAULT_PAGE_SIZE)
        );
        page.setOptimizeCountSql(false);
        return exportCenterManage.pager(page, normalizeStatus(status));
    }

    private int positiveIntOrDefault(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "undefined".equalsIgnoreCase(status) || "null".equalsIgnoreCase(status)) {
            return DEFAULT_STATUS;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (EXPORT_STATUSES.contains(normalized)) {
            return normalized;
        }
        return status;
    }

    @Override
    @CrestAudit(id = "#p0", ot = LogOT.DELETE, st = LogST.DATA)
    public void delete(String id) {
        exportCenterManage.delete(id);
    }

    @Override
    @CrestAudit(ot = LogOT.DELETE, st = LogST.DATA)
    public void delete(List<String> ids) {
        exportCenterManage.delete(ids);
    }

    @Override
    @CrestAudit(ot = LogOT.CLEAR, st = LogST.DATA)
    public void deleteAll(String type) {
        exportCenterManage.deleteAll(type);
    }

    @Override
    @CrestAudit(id = "#p0", ot = LogOT.DOWNLOAD, st = LogST.DATA)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void download(String id, String ticket, HttpServletResponse response) throws Exception {
        try {
            exportCenterManage.download(id, ticket, response);
        } catch (CrestException e) {
            writeForbidden(response, e);
        }
    }

    @SuppressWarnings("java/xss")
    private void writeForbidden(HttpServletResponse response, CrestException e) throws Exception {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? "下载链接无效或已过期"
                : e.getMessage();
        response.getWriter().write(JsonUtil.toJSONString(new ResultMessage(e.getCode(), message)).toString());
    }

    @Override
    @CrestAudit(id = "#p0", ot = LogOT.READ, st = LogST.DATA)
    public String generateDownloadUri(String id) throws Exception {
        return exportCenterManage.generateDownloadUri(id);
    }

    @Override
    @CrestAudit(id = "#p0", ot = LogOT.MODIFY, st = LogST.DATA)
    public void retry(String id) {
        exportCenterManage.retry(id);
    }

    public String exportLimit() {
        return String.valueOf(ExportCenterUtils.getExportLimit("dataset"));
    }
}
