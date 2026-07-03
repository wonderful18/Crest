package io.crest.security;

import io.crest.filter.HtmlResourceFilter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlResourceFilterSecurityTest {

    @Test
    void staticForwardPathAllowsOnlyVersionedFrontendAssets() throws Exception {
        HtmlResourceFilter filter = new HtmlResourceFilter();
        Method method = HtmlResourceFilter.class.getDeclaredMethod("isSafeStaticForwardPath", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(filter, "js/app-2.1.0-crest.js"));
        assertFalse((Boolean) method.invoke(filter, "js/../app-2.1.0-crest.js"));
        assertFalse((Boolean) method.invoke(filter, "js\\app-2.1.0-crest.js"));
        assertFalse((Boolean) method.invoke(filter, "WEB-INF/web.xml"));
    }
}
