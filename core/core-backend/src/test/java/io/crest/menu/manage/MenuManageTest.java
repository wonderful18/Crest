package io.crest.menu.manage;

import io.crest.api.menu.vo.MenuVO;
import io.crest.auth.bo.TokenUserBO;
import io.crest.i18n.Translator;
import io.crest.menu.dao.auto.entity.CoreMenu;
import io.crest.substitute.permissions.auth.PlatformPermissionManage;
import io.crest.utils.AuthUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuManageTest {

    @AfterEach
    void tearDown() {
        AuthUtils.remove();
    }

    @Test
    void internalLiteMenuShouldIncludeDataAssetEntry() {
        PlatformPermissionManage permissionManage = mock(PlatformPermissionManage.class);
        MenuManage menuManage = new MenuManage();
        ReflectionTestUtils.setField(menuManage, "platformPermissionManage", permissionManage);
        ReflectionTestUtils.setField(menuManage, "internalLiteEnabled", true);
        ReflectionTestUtils.setField(Translator.class, "messageSource", new StaticMessageSource());
        AuthUtils.setUser(new TokenUserBO(1L, 1L));
        when(permissionManage.isSystemAdmin(1L)).thenReturn(true);

        List<MenuVO> menus = menuManage.query(List.of(
                menu(4L, 0L, 1, "data", "/data", null, 2),
                menu(77L, 4L, 2, "data-asset", "/asset", "visualized/data/asset", 3)
        ));

        assertEquals("data", menus.getFirst().getName());
        assertEquals("data-asset", menus.getFirst().getChildren().getFirst().getName());
        assertEquals("asset", menus.getFirst().getChildren().getFirst().getPath());
    }

    private static CoreMenu menu(Long id, Long pid, Integer type, String name, String path, String component,
            Integer sort) {
        CoreMenu menu = new CoreMenu();
        menu.setId(id);
        menu.setPid(pid);
        menu.setType(type);
        menu.setName(name);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setMenuSort(sort);
        menu.setHidden(false);
        menu.setInLayout(true);
        menu.setAuth(true);
        return menu;
    }
}
