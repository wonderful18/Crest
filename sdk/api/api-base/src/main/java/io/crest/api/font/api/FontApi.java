package io.crest.api.font.api;

import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.crest.api.font.dto.FontDto;
import io.crest.exception.CrestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Tag(name = "数据集管理:数据")
@ApiSupport(order = 972)
public interface FontApi {

    @Operation(summary = "预览数据")
    @GetMapping("fonts")
    List<FontDto> list();

    @Operation(summary = "创建")
    @PostMapping
    public FontDto create(@RequestBody FontDto fontDto);

    @Operation(summary = "编辑")
    @PutMapping
    public FontDto edit(@RequestBody FontDto fontDto);

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id);

    @Operation(summary = "变更默认设置")
    @PostMapping("/default/")
    public void changeDefault(@RequestBody FontDto fontDto);

    @PostMapping("/files/upload")
    FontDto upload(@RequestParam("file") MultipartFile file) throws CrestException;

    @GetMapping("/download/{file}")
    void download(@PathVariable("file") String file, HttpServletResponse response) throws CrestException;

    @GetMapping("/default")
    List<FontDto> defaultFont() throws CrestException;
}
