package io.crest.font.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.crest.api.font.dto.FontDto;
import io.crest.exception.CrestException;
import io.crest.font.dao.auto.entity.CoreFont;
import io.crest.font.dao.auto.mapper.CoreFontMapper;
import io.crest.utils.BeanUtils;
import io.crest.utils.CrestPermissionUtils;
import io.crest.utils.FileUtils;
import io.crest.utils.IDUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class FontManage {

    private static final long MAX_FONT_BYTES = 20L * 1024L * 1024L;
    private static final long KIB = 1024L;
    private static final long MIB = KIB * 1024L;
    private static final long GIB = MIB * 1024L;

    @Value("${crest.path.font:/opt/crest/data/font/}")
    private String path;

    @Resource
    private CoreFontMapper coreFontMapper;
    @Autowired
    private ResourceLoader resourceLoader;

    public List<FontDto> list() {
        QueryWrapper<CoreFont> queryWrapper = new QueryWrapper<>();
        List<CoreFont> coreFonts = coreFontMapper.selectList(queryWrapper);
        List<FontDto> fontDtos = new ArrayList<>();
        for (CoreFont coreFont : coreFonts) {
            FontDto dto = new FontDto();
            BeanUtils.copyBean(dto, coreFont);
            fontDtos.add(dto);
        }

        return fontDtos;
    }

    public FontDto create(FontDto fontDto) {
        CrestPermissionUtils.requireAdmin();
        QueryWrapper<CoreFont> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", fontDto.getName());
        if (CollectionUtils.isNotEmpty(coreFontMapper.selectList(queryWrapper))) {
            CrestException.throwException("存在重名字库");
        }
        fontDto.setId(IDUtils.snowID());
        CoreFont coreFont = new CoreFont();
        BeanUtils.copyBean(coreFont, fontDto);
        coreFont.setUpdateTime(System.currentTimeMillis());
        coreFontMapper.insert(coreFont);
        return fontDto;
    }


    public FontDto edit(FontDto fontDto) {
        CrestPermissionUtils.requireAdmin();
        if (ObjectUtils.isEmpty(fontDto.getId())) {
            return create(fontDto);
        }
        if (fontDto.getIsDefault()) {
            UpdateWrapper<CoreFont> updateWrapper = new UpdateWrapper<>();
            updateWrapper.ne("id", fontDto.getId());
            CoreFont record = new CoreFont();
            record.setIsDefault(false);
            coreFontMapper.update(record, updateWrapper);
        }
        CoreFont coreFont = new CoreFont();
        BeanUtils.copyBean(coreFont, fontDto);
        coreFont.setUpdateTime(System.currentTimeMillis());
        coreFontMapper.updateById(coreFont);
        return fontDto;
    }

    public void delete(Long id) {
        CrestPermissionUtils.requireAdmin();
        CoreFont coreFont = coreFontMapper.selectById(id);
        if (coreFont != null) {
            coreFontMapper.deleteById(id);
            if (StringUtils.isNotEmpty(coreFont.getFileTransName())) {
                try {
                    FileUtils.deleteFile(FileUtils.resolveUnderDirectory(path, coreFont.getFileTransName()).getPath());
                } catch (IOException e) {
                    CrestException.throwException(e.getMessage());
                }
            }
        }

    }

    public void changeDefault(FontDto fontDto) {
        CrestPermissionUtils.requireAdmin();
        QueryWrapper<CoreFont> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", fontDto.getId());
        CoreFont record = new CoreFont();
        record.setIsDefault(fontDto.getIsDefault());
        coreFontMapper.update(record, queryWrapper);
    }

    public FontDto upload(MultipartFile file) {
        CrestPermissionUtils.requireAdmin();
        String fileUuid = UUID.randomUUID().toString();
        return saveFile(file, fileUuid);
    }

    public void download(String file, HttpServletResponse response) {

        QueryWrapper<CoreFont> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_trans_name", file);
        List<CoreFont> coreFonts = coreFontMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(coreFonts)) {
            CrestException.throwException("不存在的字库文件");
        }

        try {
            response.setContentType("application/x-download");
            String fileTransName = coreFonts.get(0).getFileTransName();
            response.setHeader("Content-Disposition", "attachment;filename=" + fileTransName);
            try (ServletOutputStream out = response.getOutputStream();
                 InputStream stream = new FileInputStream(FileUtils.resolveUnderDirectory(path, fileTransName))) {
                byte buff[] = new byte[1024];
                int length;
                while ((length = stream.read(buff)) > 0) {
                    out.write(buff, 0, length);
                }
                out.flush();
            }
        } catch (IOException e) {
            CrestException.throwException(e.getMessage());
        }
    }

    public List<FontDto> defaultFont() {
        QueryWrapper<CoreFont> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_default", 1);
        List<CoreFont> coreFonts = coreFontMapper.selectList(queryWrapper);
        List<FontDto> fontDtos = new ArrayList<>();
        for (CoreFont coreFont : coreFonts) {
            FontDto dto = new FontDto();
            BeanUtils.copyBean(dto, coreFont);
            fontDtos.add(dto);
        }
        return fontDtos;
    }

    @SuppressWarnings("java/path-injection")
    private FontDto saveFile(MultipartFile file, String fileNameUUID) throws CrestException {
        FontDto fontDto = new FontDto();
        File targetFile = null;
        try {
            if (file == null || file.isEmpty()) {
                CrestException.throwException("字体文件不能为空");
            }
            FileUtils.validateUploadFileSize(file, MAX_FONT_BYTES, "字体文件");
            String filename = file.getOriginalFilename();
            if (StringUtils.isEmpty(filename) || !filename.toLowerCase(Locale.ROOT).endsWith(".ttf")) {
                CrestException.throwException("非法格式的文件！");
            }
            FileUtils.validateUploadFilename(filename);
            String suffix = filename.substring(filename.lastIndexOf('.') + 1);
            File f = FileUtils.resolveUnderDirectory(path, fileNameUUID + "." + suffix);
            targetFile = f;
            try (FileOutputStream fileOutputStream = new FileOutputStream(f)) {
                fileOutputStream.write(file.getBytes());
                fileOutputStream.flush();
            }
            fontDto.setFileTransName(fileNameUUID + "." + suffix);

            long length = file.getSize();
            String unit;
            double size;
            if (length > MIB) {
                if (length > GIB) {
                    unit = "GB";
                    size = displaySize(length, GIB);
                } else {
                    unit = "MB";
                    size = displaySize(length, MIB);
                }
            } else {
                unit = "KB";
                size = displaySize(length, KIB);
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, f);
            fontDto.setSize(size);
            fontDto.setSizeType(unit);
            fontDto.setName(font.getFontName());
        } catch (IOException | FontFormatException | RuntimeException e) {
            if (targetFile != null && targetFile.exists()) {
                FileUtils.deleteFile(targetFile.getPath());
            }
            CrestException.throwException(e);
        }
        return fontDto;
    }

    private double displaySize(long length, long unitBytes) {
        return BigDecimal.valueOf(length)
                .divide(BigDecimal.valueOf(unitBytes), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
