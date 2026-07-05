package io.crest.visualization.server;

import io.crest.exception.CrestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaticResourceServerTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadAcceptsValidMultipartPng() throws IOException {
        StaticResourceServer server = server();
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
        MockMultipartFile file = new MockMultipartFile("file", "background.png", "image/png", png);

        server.upload("bg-1", file);

        assertArrayEquals(png, Files.readAllBytes(tempDir.resolve("bg-1.png")));
    }

    @Test
    void saveSingleFileToServeAcceptsDataUriBase64Image() throws IOException {
        StaticResourceServer server = server();
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");

        server.saveSingleFileToServe("logo.png", "data:image/png;base64," + Base64.getEncoder().encodeToString(png));

        assertArrayEquals(png, Files.readAllBytes(tempDir.resolve("logo.png")));
    }

    @Test
    void saveSingleFileToServeRejectsDangerousSvgDataUri() {
        StaticResourceServer server = server();
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>";
        String dataUri = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

        CrestException exception = assertThrows(CrestException.class, () -> server.saveSingleFileToServe("logo.svg", dataUri));

        assertEquals("SVG包含不允许的脚本或事件处理器", exception.getMsg());
    }

    @Test
    void saveSingleFileToServeRejectsSvgDoctype() {
        StaticResourceServer server = server();
        String svg = """
                <!DOCTYPE svg [
                  <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <svg xmlns="http://www.w3.org/2000/svg">&xxe;</svg>
                """;
        String dataUri = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

        CrestException exception = assertThrows(CrestException.class, () -> server.saveSingleFileToServe("logo.svg", dataUri));

        assertEquals("svg 内容禁止使用 DOCTYPE", exception.getMsg());
    }

    @Test
    void saveSingleFileToServeRejectsSvgJavaScriptHref() {
        StaticResourceServer server = server();
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <a href="javascript:alert(1)"><text>open</text></a>
                </svg>
                """;
        String dataUri = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));

        CrestException exception = assertThrows(CrestException.class, () -> server.saveSingleFileToServe("logo.svg", dataUri));

        assertEquals("SVG包含不允许的脚本或事件处理器", exception.getMsg());
    }

    @Test
    void saveSingleFileToServeRejectsCorruptedPngDataUri() {
        StaticResourceServer server = server();
        byte[] corruptedPng = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42g==");

        CrestException exception = assertThrows(CrestException.class,
                () -> server.saveSingleFileToServe("broken.png",
                        "data:image/png;base64," + Base64.getEncoder().encodeToString(corruptedPng)));

        assertEquals("静态资源必须是图片", exception.getMsg());
    }

    private StaticResourceServer server() {
        StaticResourceServer server = new StaticResourceServer();
        ReflectionTestUtils.setField(server, "staticDir", tempDir.toString());
        return server;
    }
}
