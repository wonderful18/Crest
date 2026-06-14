package io.crest.dataset.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldUtilsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown_type", "JSON"})
    void resolveFieldTypeDefaultsUnknownOrBlankTypesToText(String type) {
        assertEquals(0, FieldUtils.resolveFieldType(type));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "VARCHAR(255)|0",
            "CHAR(32)|0",
            "NCHAR|0",
            "longtext|0",
            "STRING|0",
            "CHARACTER VARYING|0",
            "UUID|0",
            "JSONB|0",
            "DATE|1",
            "TIME|1",
            "YEAR|1",
            "datetime(6)|1",
            "TIMESTAMP|1",
            "DATEV2|1",
            "DATETIMEV2|1",
            "DATETIME2(7)|1",
            "TIMESTAMPTZ|1",
            "DATETIMEOFFSET|1",
            "BIGINT(20)|2",
            "INTEGER|2",
            "INT4|2",
            "UINT64|2",
            "INT64|2",
            "SMALLINT UNSIGNED|2",
            "MEDIUMINT|2",
            "DECIMAL(18, 4)|3",
            "NUMERIC(12,2)|3",
            "NUMBER(10,0)|3",
            "double precision|3",
            "FLOAT64|3",
            "DECFLOAT|3",
            "FLOAT8|3",
            "REAL|3",
            "MONEY|3",
            "TINYINT|4",
            "TINYINT(1)|4",
            "BIT|4",
            "BIT(1)|4",
            "BOOL|0",
            "BOOLEAN|0",
            "123|0"
    })
    void resolveFieldTypeHandlesCommonExcelAndDatabaseTypes(String type, int expected) {
        assertEquals(expected, FieldUtils.resolveFieldType(type));
    }

    @Test
    void resolveFieldGroupKeepsDimensionsAndMetricsSeparated() {
        assertEquals("d", FieldUtils.resolveFieldGroup(0));
        assertEquals("d", FieldUtils.resolveFieldGroup(1));
        assertEquals("q", FieldUtils.resolveFieldGroup(2));
        assertEquals("q", FieldUtils.resolveFieldGroup(3));
        assertEquals("q", FieldUtils.resolveFieldGroup(4));
    }
}
