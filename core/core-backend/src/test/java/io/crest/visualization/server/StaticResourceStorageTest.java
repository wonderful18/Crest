package io.crest.visualization.server;

import io.crest.api.visualization.request.StaticResourceRequest;
import io.crest.api.visualization.request.DataVisualizationBaseRequest;
import io.crest.exception.CrestException;
import io.crest.storage.LocalStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticResourceStorageTest {

    private static final String SVG_BASE64 = "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxIiBoZWlnaHQ9IjEiPjwvc3ZnPg==";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("静态资源保存应写入存储服务目录")
    void saveShouldWriteStaticResourceToStorageService() throws Exception {
        StaticResourceServer server = staticResourceServer();

        server.saveSingleFileToServe("sample.svg", SVG_BASE64);

        assertThat(tempDir.resolve("sample.svg")).exists();
        assertThat(Files.readAllBytes(tempDir.resolve("sample.svg"))).isNotEmpty();
    }

    @Test
    @DisplayName("静态资源读取应通过存储服务返回 Base64")
    void resourceBase64ShouldReadFromStorageService() {
        StaticResourceServer server = staticResourceServer();
        server.saveSingleFileToServe("sample.svg", SVG_BASE64);
        StaticResourceRequest request = new StaticResourceRequest();
        request.setResourcePathList(List.of("/static-resource/sample.svg"));

        Map<String, String> result = server.resourceBase64(request);

        assertThat(result).containsEntry("/static-resource/sample.svg", SVG_BASE64);
    }

    @Test
    @DisplayName("模板文件应接受 .crest 后缀")
    void crestTemplateFileExtensionShouldBeAcceptedCaseInsensitively() {
        assertThat(DataVisualizationServer.isSupportedTemplateFile("sales.crest")).isTrue();
        assertThat(DataVisualizationServer.isSupportedTemplateFile("sales.CREST")).isTrue();
    }

    @Test
    @DisplayName("模板导入应兼容旧模板后缀")
    void legacyTemplateFileExtensionsShouldBeAcceptedForImportCompatibility() {
        assertThat(DataVisualizationServer.isSupportedTemplateFile("sales.DET2")).isTrue();
        assertThat(DataVisualizationServer.isSupportedTemplateFile("sales.DET2APP")).isTrue();
        assertThat(DataVisualizationServer.isSupportedTemplateFile("sales.json")).isFalse();
        assertThat(DataVisualizationServer.isSupportedTemplateFile(null)).isFalse();
    }

    @Test
    @DisplayName("模板导入应拒绝非法文件名")
    void templateImportShouldRejectUnsafeFilename() {
        DataVisualizationServer server = new DataVisualizationServer();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../sales.crest",
                "application/json",
                templateJson().getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(CrestException.class, () -> parseLocalTemplate(server, file));
    }

    @Test
    @DisplayName("模板导入应兼容旧 DET2 文件")
    void templateImportShouldAcceptLegacyDet2File() throws Exception {
        DataVisualizationServer server = new DataVisualizationServer();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.DET2",
                "application/json",
                templateJson().getBytes(StandardCharsets.UTF_8)
        );

        DataVisualizationBaseRequest request = parseLocalTemplate(server, file);

        assertThat(request.getName()).isEqualTo("sales");
        assertThat(request.getType()).isEqualTo("dataV");
    }

    @Test
    @DisplayName("模板导入应拒绝空文件")
    void templateImportShouldRejectEmptyFile() {
        DataVisualizationServer server = new DataVisualizationServer();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.crest",
                "application/json",
                new byte[0]
        );

        assertThrows(CrestException.class, () -> parseLocalTemplate(server, file));
    }

    private StaticResourceServer staticResourceServer() {
        StaticResourceServer server = new StaticResourceServer();
        ReflectionTestUtils.setField(server, "staticDir", tempDir.toString());
        ReflectionTestUtils.setField(server, "storageService", new LocalStorageService());
        return server;
    }

    private DataVisualizationBaseRequest parseLocalTemplate(DataVisualizationServer server, MockMultipartFile file) {
        return ReflectionTestUtils.invokeMethod(server, "parseLocalTemplate", file);
    }

    private String templateJson() {
        return """
                {
                  "name": "sales",
                  "dvType": "dataV",
                  "version": 3,
                  "canvasStyleData": "{}",
                  "componentData": "[]",
                  "dynamicData": "{}",
                  "staticResource": "{}"
                }
                """;
    }
}
