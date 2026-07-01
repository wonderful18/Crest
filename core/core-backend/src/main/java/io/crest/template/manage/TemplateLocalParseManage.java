package io.crest.template.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import io.crest.utils.LogUtil;
import io.crest.template.dao.auto.entity.TemplateVersion;
import io.crest.template.dao.auto.mapper.TemplateVersionMapper;
import io.crest.utils.JsonUtil;
import io.crest.visualization.server.StaticResourceServer;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.repository.init.ResourceReader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 初始化内置模板资源，并记录模板脚本的执行状态。
 */
@Service
public class TemplateLocalParseManage {

    @Resource
    private StaticResourceServer staticResourceServer;

    @Resource
    private TemplateVersionMapper templateVersionMapper;

    @Resource(type = ResourceLoader.class)
    private ResourceLoader resourceLoader;

    public void doInit() throws Exception {
        org.springframework.core.io.Resource[] templateFiles = getAllFilesInResourceDirectory("template");
        if (templateFiles != null && templateFiles.length > 0) {
            for (int i = 0; i < templateFiles.length; i++) {
                org.springframework.core.io.Resource templateFile = templateFiles[i];
                String templateName = templateFile.getFilename();
                QueryWrapper<TemplateVersion> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("script", templateName);
                if (!templateVersionMapper.exists(queryWrapper)) {
                    TemplateVersion version = new TemplateVersion();
                    version.setScript(templateName);
                    version.setInstalledOn(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
                    try (InputStream inputStream = templateFile.getInputStream()) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        DataVisualizationBaseRequest template = JsonUtil.parseObject(content, DataVisualizationBaseRequest.class);
                        parseCore(template);
                        version.setSuccess(true);
                        templateVersionMapper.insert(version);
                    } catch (Exception e) {
                        LogUtil.error("De Template Version Error : " + templateName);
                        version.setSuccess(false);
                        templateVersionMapper.insert(version);
                        break;
                    }
                }

            }
        }
    }

    public void parseCore(DataVisualizationBaseRequest template) {
        // 保存模板携带的静态文件，供模板预览和复制后复用。
        staticResourceServer.saveFilesToServe(template.getStaticResource());
    }


    public org.springframework.core.io.Resource[] getAllFilesInResourceDirectory(String directoryName) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);

        // 当前内置模板固定加载 classpath:template 目录。
        org.springframework.core.io.Resource[] resources = resolver.getResources("classpath:template/*");

        return resources;
    }

    public static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(file.toPath());
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

}
