package io.crest.engine.trans;

import io.crest.constant.FieldTypeConstants;
import io.crest.extensions.datasource.dto.DatasetTableFieldDTO;

import java.util.Objects;

final class FieldSqlExpressionUtils {

    private FieldSqlExpressionUtils() {
    }

    static String nullSafeTextOrigin(DatasetTableFieldDTO field, String originField) {
        if (!needsTextNullNormalization(field)) {
            return originField;
        }
        return "CASE WHEN " + originField + " IS NULL THEN NULL "
                + "WHEN LOWER(TRIM(" + originField + ")) IN ('', 'null', '\\\\n') THEN NULL "
                + "ELSE TRIM(" + originField + ") END";
    }

    private static boolean needsTextNullNormalization(DatasetTableFieldDTO field) {
        if (field == null) {
            return false;
        }
        boolean textBacked = Objects.equals(field.getExtractedFieldType(), FieldTypeConstants.STRING)
                || Objects.equals(field.getExtractedFieldType(), FieldTypeConstants.LOCATION);
        boolean converted = Objects.equals(field.getFieldType(), FieldTypeConstants.TIME)
                || Objects.equals(field.getFieldType(), FieldTypeConstants.INTEGER)
                || Objects.equals(field.getFieldType(), FieldTypeConstants.FLOAT)
                || Objects.equals(field.getFieldType(), FieldTypeConstants.BOOLEAN);
        return textBacked && converted;
    }
}
