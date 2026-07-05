package io.crest.template.service;

import io.crest.api.template.request.TemplateManageRequest;
import io.crest.auth.bo.TokenUserBO;
import io.crest.exception.CrestException;
import io.crest.template.dao.auto.mapper.VisualizationTemplateCategoryMapper;
import io.crest.template.dao.auto.mapper.VisualizationTemplateMapper;
import io.crest.template.dao.ext.ExtVisualizationTemplateMapper;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateManageServiceAccessTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void managementReadEndpointsRequireAdmin() {
        AuthUtils.setUser(new TokenUserBO(2L, 1L));
        TemplateManageService service = serviceWithMocks();
        TemplateManageRequest request = new TemplateManageRequest();
        request.setTemplateArray(List.of("template-1"));
        request.setTemplateNames(List.of("template-name"));
        request.setCategories(List.of("category-1"));

        assertAdminRequired(() -> service.templateList(request));
        assertAdminRequired(() -> service.find(request));
        assertAdminRequired(() -> service.findCategories(request));
        assertAdminRequired(() -> service.findOne("template-1"));
        assertAdminRequired(() -> service.findCategoriesByTemplateIds(request));
        assertAdminRequired(() -> service.nameCheck(request));
        assertAdminRequired(() -> service.categoryTemplateNameCheck(request));
        assertAdminRequired(() -> service.checkCategoryTemplateBatchNames(request));
    }

    private void assertAdminRequired(Runnable action) {
        CrestException exception = assertThrows(CrestException.class, action::run);
        assertEquals("当前用户没有管理员权限", exception.getMsg());
    }

    private TemplateManageService serviceWithMocks() {
        TemplateManageService service = new TemplateManageService();
        ExtVisualizationTemplateMapper extTemplateMapper = mock(ExtVisualizationTemplateMapper.class);
        VisualizationTemplateMapper templateMapper = mock(VisualizationTemplateMapper.class);
        VisualizationTemplateCategoryMapper categoryMapper = mock(VisualizationTemplateCategoryMapper.class);
        when(extTemplateMapper.findTemplateList(any())).thenReturn(List.of());
        when(extTemplateMapper.findCategories(any())).thenReturn(List.of());
        when(extTemplateMapper.findTemplateArrayCategories(anyList())).thenReturn(List.of());
        when(extTemplateMapper.checkCategoryTemplateName(any(), anyList())).thenReturn(0L);
        when(extTemplateMapper.checkCategoryTemplateBatchNames(anyList(), anyList(), anyList())).thenReturn(0L);
        when(templateMapper.selectList(any())).thenReturn(List.of());
        when(categoryMapper.selectList(any())).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "extTemplateMapper", extTemplateMapper);
        ReflectionTestUtils.setField(service, "templateMapper", templateMapper);
        ReflectionTestUtils.setField(service, "templateCategoryMapper", categoryMapper);
        return service;
    }
}
