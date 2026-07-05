package io.crest.visualization.server;

import io.crest.api.visualization.StaticResourceApi;
import io.crest.api.visualization.request.StaticResourceRequest;
import io.crest.exception.CrestException;
import io.crest.utils.FileUtils;
import io.crest.utils.JsonUtil;
import io.crest.utils.LogUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/static-resource")
@SuppressWarnings("unchecked")
public class StaticResourceServer implements StaticResourceApi {

    private static final long MAX_STATIC_RESOURCE_BYTES = 20L * 1024L * 1024L;
    private static final Pattern BASE64_SCRIPT_PAYLOAD_PATTERN = Pattern.compile(".*base64\\s*,\\s*[a-z0-9+/=]{20,}.*");

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".gif", ".svg", ".png", ".jpeg", ".jpg"
    );

    private static final Set<String> ALLOWED_SVG_MIME_TYPES = Set.of(
            "image/svg+xml",
            "image/svg-xml",
            "application/svg+xml"
    );

    @Value("${crest.path.static-resource:/opt/crest/data/static-resource/}")
    private String staticDir;

    @Override
    public void upload(String fileId, MultipartFile file) {
        // check if the path is valid (not outside staticDir)
        Assert.notNull(file, "Multipart file must not be null");
        try {
            FileUtils.validateUploadFileSize(file, MAX_STATIC_RESOURCE_BYTES, "静态资源文件");
            String originName = file.getOriginalFilename();
            validateImageFilename(originName);
            String newFileName = fileId + originName.substring(originName.lastIndexOf('.'), originName.length());
            validateImageFilename(newFileName);
            byte[] fileBytes = file.getBytes();
            validateImageContent(newFileName, fileBytes, file.getContentType(), true);
            writeFileIfAbsent(newFileName, fileBytes);
        } catch (CrestException e) {
            throw e;
        } catch (IOException e) {
            LogUtil.error("文件上传失败", e);
            CrestException.throwException("文件上传失败");
        } catch (Exception e) {
            CrestException.throwException(e);
        }
    }

    private boolean hasValidImageExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return false;
        }
        String lowerFilename = filename.toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    public void saveFilesToServe(String staticResource) {
        if (StringUtils.isNotEmpty(staticResource)) {
            Map<String, String> resource = JsonUtil.parse(staticResource, Map.class);
            if (resource == null || resource.isEmpty()) {
                return;
            }
            for (Map.Entry<String, String> entry : resource.entrySet()) {
                String path = entry.getKey();
                String fileName = extractFileName(path);
                saveSingleFileToServe(fileName, entry.getValue());
            }
        }
    }

    public void saveSingleFileToServe(String fileName, String content) {
        try {
            validateImageFilename(fileName);
            if (StringUtils.isEmpty(content)) {
                CrestException.throwException("静态资源内容不能为空");
            }
            byte[] fileBytes = decodeBase64Content(content);
            FileUtils.validateContentSize(fileBytes, MAX_STATIC_RESOURCE_BYTES, "静态资源文件");
            validateImageContent(fileName, fileBytes, null, false);
            writeFileIfAbsent(fileName, fileBytes);
        } catch (CrestException e) {
            throw e;
        } catch (Exception e) {
            LogUtil.error("template static resource save error", e);
            CrestException.throwException(e);
        }
    }

    @Override
    public Map<String, String> resourceBase64(@RequestBody StaticResourceRequest resourceRequest) {
        Map<String, String> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(resourceRequest.getResourcePathList())) {
            for (String path : resourceRequest.getResourcePathList()) {
                String value = readResourceAsBase64(extractFileName(path));
                result.put(path, value);
            }
        }
        return result;
    }

    private void validateImageFilename(String filename) {
        FileUtils.validateUploadFilename(filename);
        if (!hasValidImageExtension(filename)) {
            CrestException.throwException("静态资源必须是图片");
        }
    }

    private String extractFileName(String path) {
        return StringUtils.substringAfterLast(path.replace("\\", "/"), "/");
    }

    private String readResourceAsBase64(String fileName) {
        if (StringUtils.isBlank(fileName) || !hasValidImageExtension(fileName)) {
            return null;
        }
        try {
            FileUtils.validateUploadFilename(fileName);
            File resourceFile = FileUtils.resolveUnderDirectory(staticDir, fileName);
            Path resourcePath = resourceFile.toPath();
            if (!Files.isRegularFile(resourcePath)) {
                return null;
            }
            return Base64.getEncoder().encodeToString(Files.readAllBytes(resourcePath));
        } catch (RuntimeException e) {
            return null;
        } catch (IOException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            return null;
        }
    }

    private byte[] decodeBase64Content(String content) {
        String base64Content = content.trim();
        if (base64Content.regionMatches(true, 0, "data:", 0, 5)) {
            int commaIndex = base64Content.indexOf(',');
            if (commaIndex < 0 || !base64Content.substring(0, commaIndex).toLowerCase(Locale.ROOT).contains(";base64")) {
                CrestException.throwException("静态资源Base64内容无效");
            }
            base64Content = base64Content.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            CrestException.throwException("静态资源Base64内容无效");
            return new byte[0];
        }
    }

    private void writeFileIfAbsent(String fileName, byte[] content) throws IOException {
        Path uploadPath = FileUtils.resolveUnderDirectory(staticDir, fileName).toPath();
        if (Files.exists(uploadPath)) {
            LogUtil.info("file exists");
            return;
        }
        Files.write(uploadPath, content, StandardOpenOption.CREATE_NEW);
    }

    private void validateImageContent(String fileName, byte[] content, String mimeType, boolean checkSvgMimeType) {
        if (content == null || content.length == 0) {
            CrestException.throwException("静态资源内容不能为空");
        }
        if (Strings.CI.endsWith(fileName, ".svg")) {
            validateSvgContent(content, mimeType, checkSvgMimeType);
            return;
        }
        if (!isImageOther(content)) {
            CrestException.throwException("静态资源必须是图片");
        }
    }

    private boolean isImageOther(byte[] content) {
        try (InputStream input = new ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(input);
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException e) {
            LogUtil.debug(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            return false;
        }
    }

    private void validateSvgContent(byte[] content, String mimeType, boolean checkSvgMimeType) {
        if (checkSvgMimeType && !isValidSvgMimeType(mimeType)) {
            CrestException.throwException("无效的SVG文件MIME类型");
        }
        try (InputStream inputStream = new ByteArrayInputStream(content)) {
            DocumentBuilder db = secureSvgDocumentBuilderFactory().newDocumentBuilder();
            db.setErrorHandler(silentSvgErrorHandler());
            Document doc = db.parse(inputStream);
            if (!"svg".equals(doc.getDocumentElement().getNodeName())) {
                CrestException.throwException("根元素必须是svg");
            }
            if (containsDangerousContent(doc)) {
                CrestException.throwException("SVG包含不允许的脚本或事件处理器");
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("DOCTYPE")) {
                CrestException.throwException("svg 内容禁止使用 DOCTYPE");
            } else {
                CrestException.throwException("SVG解析失败: " + e.getMessage());
            }
        }
    }

    private static boolean isValidSvgMimeType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return ALLOWED_SVG_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }

    private static DocumentBuilderFactory secureSvgDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }

    private static ErrorHandler silentSvgErrorHandler() {
        return new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        };
    }

    /**
     * 检查SVG是否包含危险内容
     * @return true 包含危险内容，false 安全
     */
    private static boolean containsDangerousContent(Document doc) {
        // 危险的事件处理器属性
        Set<String> dangerousAttributes = new HashSet<>(Arrays.asList(
                "onload", "onerror", "onclick", "ondblclick", "onmousedown", "onmouseup",
                "onmouseover", "onmousemove", "onmouseout", "onfocus", "onblur",
                "onkeydown", "onkeypress", "onkeyup", "onsubmit", "onreset",
                "onchange", "onselect", "onabort", "onunload", "onresize",
                "onscroll", "oninput", "onactivate", "onbeforeactivate", "onbeforedeactivate",
                "ondeactivate", "onbegin", "onend", "onrepeat", "onloadstart",
                "onprogress", "onloadend", "oncanplay", "oncanplaythrough", "onwaiting",
                "onseeking", "onseeked", "ontimeupdate", "onplaying", "onpause",
                "onratechange", "ondurationchange", "onvolumechange"
        ));

        // 危险标签
        Set<String> dangerousTags = new HashSet<>(Arrays.asList(
                "script", "style", "object", "embed", "applet", "iframe",
                "frame", "frameset", "link", "meta", "base", "form"
        ));

        // 遍历所有元素
        NodeList elements = doc.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String tagName = element.getTagName().toLowerCase(Locale.ROOT);

            // 检查危险标签
            if (dangerousTags.contains(tagName)) {
                return true; // 发现危险标签，返回true表示包含危险内容
            }

            // 检查属性
            NamedNodeMap attributes = element.getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Attr attr = (Attr) attributes.item(j);
                String attrName = attr.getName().toLowerCase(Locale.ROOT);
                String attrValue = attr.getValue().toLowerCase(Locale.ROOT);

                // 检查事件处理器属性
                if (dangerousAttributes.contains(attrName)) {
                    return true;
                }

                // 检查属性名是否以"on"开头（事件处理器）
                if (attrName.startsWith("on") && attrName.length() > 2) {
                    return true;
                }

                // 检查属性值是否包含JavaScript相关代码
                if (containsJavaScript(attrValue)) {
                    return true;
                }
            }
        }

        // 检查文本节点
        if (containsScriptInText(doc)) {
            return true;
        }

        return false;
    }

    /**
     * 检查字符串是否包含JavaScript代码
     */
    private static boolean containsJavaScript(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        String lowerValue = value.toLowerCase(Locale.ROOT);

        // JavaScript相关模式
        String[] jsPatterns = {
                "javascript:", "vbscript:", "data:", "expression(",
                "eval(", "alert(", "confirm(", "prompt(",
                "document.", "window.", "location.", "cookie.",
                "function(", "new function", "settimeout(", "setinterval(",
                "innerhtml", "outerhtml", "insertadjacenthtml",
                "<script", "</script", "&#", "\\u"
        };

        for (String pattern : jsPatterns) {
            if (lowerValue.contains(pattern)) {
                return true;
            }
        }

        // 检查base64编码的潜在脚本
        if (lowerValue.contains("base64")) {
            // 简单的base64解码检查，这里可以根据需要实现更复杂的检测
            return BASE64_SCRIPT_PAYLOAD_PATTERN.matcher(lowerValue).matches();
        }

        return false;
    }

    /**
     * 检查文本节点是否包含脚本
     */
    private static boolean containsScriptInText(Document doc) {
        NodeList allNodes = doc.getElementsByTagName("*");
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);

            // 检查元素的文本内容
            String textContent = node.getTextContent();
            if (textContent != null && !textContent.trim().isEmpty()) {
                String lowerText = textContent.toLowerCase(Locale.ROOT);

                // 检查文本中是否包含HTML/XML标签
                if (lowerText.contains("<script") ||
                        lowerText.contains("</script>") ||
                        lowerText.contains("<?") ||
                        lowerText.contains("<!")) {
                    return true;
                }

                // 检查是否包含JavaScript代码
                if (containsJavaScript(lowerText)) {
                    return true;
                }
            }

            // 检查CDATA节点
            NodeList childNodes = node.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node child = childNodes.item(j);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    String cdataContent = child.getTextContent().toLowerCase(Locale.ROOT);
                    if (containsJavaScript(cdataContent) ||
                            cdataContent.contains("<script") ||
                            cdataContent.contains("</script")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
