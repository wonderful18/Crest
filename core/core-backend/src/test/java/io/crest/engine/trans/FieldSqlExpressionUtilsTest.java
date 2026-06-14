package io.crest.engine.trans;

import io.crest.constant.FieldTypeConstants;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FieldSqlExpressionUtilsTest {

    @ParameterizedTest
    @MethodSource("convertedTextBackedFields")
    void nullSafeTextOriginNormalizesBlankNullAndBackslashNForConvertedExcelTextFields(
            Integer fieldType,
            Integer extractedFieldType
    ) {
        DatasetTableFieldDTO field = field(fieldType, extractedFieldType);

        String sql = FieldSqlExpressionUtils.nullSafeTextOrigin(field, "t_a_0.`f_value`");

        assertTrue(sql.contains("CASE WHEN t_a_0.`f_value` IS NULL THEN NULL"), sql);
        assertTrue(sql.contains("LOWER(TRIM(t_a_0.`f_value`)) IN ('', 'null', '\\\\n')"), sql);
        assertTrue(sql.contains("ELSE TRIM(t_a_0.`f_value`) END"), sql);
    }

    @ParameterizedTest
    @MethodSource("fieldsThatShouldKeepOrigin")
    void nullSafeTextOriginKeepsOriginForFieldsThatDoNotNeedExcelTextConversion(
            Integer fieldType,
            Integer extractedFieldType
    ) {
        DatasetTableFieldDTO field = field(fieldType, extractedFieldType);

        String sql = FieldSqlExpressionUtils.nullSafeTextOrigin(field, "t_a_0.`f_value`");

        assertEquals("t_a_0.`f_value`", sql);
    }

    @Test
    void nullSafeTextOriginKeepsOriginWhenFieldIsMissing() {
        String sql = FieldSqlExpressionUtils.nullSafeTextOrigin(null, "t_a_0.`f_value`");

        assertEquals("t_a_0.`f_value`", sql);
        assertFalse(sql.contains("CASE WHEN"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> convertedTextBackedFields() {
        return Stream.of(
                arguments(FieldTypeConstants.TIME, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.INTEGER, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.FLOAT, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.BOOLEAN, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.TIME, FieldTypeConstants.LOCATION),
                arguments(FieldTypeConstants.INTEGER, FieldTypeConstants.LOCATION),
                arguments(FieldTypeConstants.FLOAT, FieldTypeConstants.LOCATION),
                arguments(FieldTypeConstants.BOOLEAN, FieldTypeConstants.LOCATION)
        );
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> fieldsThatShouldKeepOrigin() {
        return Stream.of(
                arguments(FieldTypeConstants.STRING, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.LOCATION, FieldTypeConstants.STRING),
                arguments(FieldTypeConstants.TIME, FieldTypeConstants.TIME),
                arguments(FieldTypeConstants.INTEGER, FieldTypeConstants.INTEGER),
                arguments(FieldTypeConstants.FLOAT, FieldTypeConstants.FLOAT),
                arguments(FieldTypeConstants.BOOLEAN, FieldTypeConstants.BOOLEAN)
        );
    }

    private static DatasetTableFieldDTO field(Integer fieldType, Integer extractedFieldType) {
        DatasetTableFieldDTO field = new DatasetTableFieldDTO();
        field.setFieldType(fieldType);
        field.setExtractedFieldType(extractedFieldType);
        return field;
    }
}
