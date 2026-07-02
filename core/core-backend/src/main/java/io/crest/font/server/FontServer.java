package io.crest.font.server;

import io.crest.api.font.api.FontApi;
import io.crest.api.font.dto.FontDto;
import io.crest.exception.CrestException;
import io.crest.font.manage.FontManage;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/typeface")
public class FontServer implements FontApi {

    @Resource
    private FontManage fontManage;

    @Override
    public List<FontDto> list() {
        return fontManage.list();
    }

    @Override
    public FontDto create(@RequestBody FontDto fontDto) {
        return fontManage.create(fontDto);
    }

    @Override
    public FontDto edit(@RequestBody FontDto fontDto) {
        return fontManage.edit(fontDto);
    }

    @Override
    public void delete(Long id) {
        fontManage.delete(id);
    }

    @Override
    public void changeDefault(@RequestBody FontDto fontDto) {
        fontManage.changeDefault(fontDto);
    }

    @Override
    public FontDto upload(@RequestParam("file") MultipartFile file) throws CrestException {
        return fontManage.upload(file);
    }

    @Override
    public void download(@PathVariable("file") String file, HttpServletResponse response) throws CrestException {
        fontManage.download(file, response);
    }

    @Override
    public List<FontDto> defaultFont() throws CrestException {
        return fontManage.defaultFont();
    }
}
