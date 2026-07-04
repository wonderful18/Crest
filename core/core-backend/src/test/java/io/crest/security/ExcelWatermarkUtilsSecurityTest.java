package io.crest.security;

import io.crest.commons.utils.ExcelWatermarkUtils;
import io.crest.visualization.dto.WatermarkContentDTO;
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

    @Test
    void transContentHandlesMissingType() {
        assertDoesNotThrow(() -> ExcelWatermarkUtils.transContent(new WatermarkContentDTO(), null));
    }
}
