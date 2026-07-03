package io.crest.security;

import io.crest.commons.utils.ExcelWatermarkUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelWatermarkUtilsSecurityTest {

    @Test
    void createTextImageHandlesMissingConfigAndLongText() {
        String longText = "watermark".repeat(200);

        byte[] image = assertDoesNotThrow(() -> ExcelWatermarkUtils.createTextImage(longText, null));

        assertTrue(image.length > 0);
    }
}
