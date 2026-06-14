package io.crest.dataset.manage;

import io.crest.extensions.datasource.constant.SqlPlaceholderConstants;
import io.crest.extensions.datasource.dto.DatasourceSchemaDTO;
import io.crest.extensions.datasource.provider.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class DatasetDataManageSqlPreviewTest {

    @Test
    void previewSqlTranslatesCrestDateFunctionsAfterTablePlaceholderReplacement() {
        DatasetDataManage manage = new DatasetDataManage();
        Provider provider = mock(Provider.class, CALLS_REAL_METHODS);
        DatasourceSchemaDTO datasource = new DatasourceSchemaDTO();
        datasource.setType("mysql");

        String sql = "SELECT * FROM (" + SqlPlaceholderConstants.TABLE_PLACEHOLDER + ") t LIMIT 100";
        String originSql = "SELECT DE_STR_TO_DATE(`f_day`,'yyyy/MM/dd') AS day_value FROM `excel_table`";

        String translated = manage.replacePreviewTablePlaceholderAndTranslate(provider, sql, originSql, datasource);

        assertFalse(translated.contains("DE_STR_TO_DATE"));
        assertTrue(translated.contains("STR_TO_DATE(`f_day`, '%Y/%m/%d')"));
    }
}
