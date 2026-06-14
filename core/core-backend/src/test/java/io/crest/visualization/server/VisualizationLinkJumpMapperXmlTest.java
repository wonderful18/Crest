package io.crest.visualization.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualizationLinkJumpMapperXmlTest {

    @Test
    void viewTableDetailsShouldMapRawAndCrestFieldTypesSeparately() throws Exception {
        String mapper = Files.readString(Path.of("src/main/resources/mybatis/ExtVisualizationLinkJumpMapper.xml"));

        assertTrue(mapper.contains("dataset_field.type AS raw_field_type"));
        assertTrue(mapper.contains("dataset_field.field_type AS field_type"));
        assertTrue(mapper.contains("column=\"raw_field_type\" jdbcType=\"VARCHAR\" property=\"type\""));
        assertTrue(mapper.contains("column=\"field_type\" jdbcType=\"VARCHAR\" property=\"fieldType\""));
        assertFalse(mapper.contains("column=\"field_type\" jdbcType=\"VARCHAR\" property=\"type\""));
    }
}
