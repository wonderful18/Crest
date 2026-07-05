package io.crest.api.exportCenter;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.crest.auth.CrestPermit;
import io.crest.model.ExportTaskDTO;
import io.crest.auth.CrestApiPath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.crest.constant.AuthResourceEnum.DATASOURCE;

@Tag(name = "数据导出中心")
@ApiSupport(order = 971)
public interface ExportCenterApi {


    @PostMapping("/export-tasks/records")
    public Map<String, Long> exportTasks();

    @CrestPermit("m:read")
    @PostMapping("/export-tasks/{status}/{goPage}/{pageSize}")
    IPage<ExportTaskDTO> pager(@PathVariable("goPage") String goPage, @PathVariable("pageSize") String pageSize, @PathVariable String status);

    @Operation(summary = "删除单条记录")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id);

    @Operation(summary = "批量删除")
    @DeleteMapping
    public void delete(@RequestBody List<String> ids);

    @Operation(summary = "删除")
    @DeleteMapping("/all/{type}")
    public void deleteAll(@PathVariable String type);

    @Operation(summary = "下载")
    @GetMapping("/download/{id}")
    public void download(@PathVariable String id, @RequestParam(required = false) String ticket, HttpServletResponse response) throws IOException;

    @Operation(summary = "生成下载Url")
    @GetMapping("/download-tickets/{id}")
    public String generateDownloadUri(@PathVariable String id);

    @Operation(summary = "重试")
    @PostMapping("/retry/{id}")
    public void retry(@PathVariable String id);

    @PostMapping("/export-limit")
    public String exportLimit();

}
