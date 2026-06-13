package io.crest.i18n;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuI18nResourceTest {

    private static final List<String> BUNDLE_NAMES = List.of(
            "i18n/core.properties",
            "i18n/core_en_US.properties",
            "i18n/core_zh_CN.properties",
            "i18n/core_zh_TW.properties"
    );

    @Test
    void menuKeysShouldExistInAllBackendBundles() throws Exception {
        TreeSet<String> allMenuKeys = new TreeSet<>();
        for (String bundleName : BUNDLE_NAMES) {
            allMenuKeys.addAll(menuKeys(loadBundle(bundleName)));
        }

        for (String bundleName : BUNDLE_NAMES) {
            Properties bundle = loadBundle(bundleName);
            TreeSet<String> missingKeys = new TreeSet<>(allMenuKeys);
            missingKeys.removeAll(menuKeys(bundle));

            assertTrue(missingKeys.isEmpty(), bundleName + " missing menu i18n keys: " + missingKeys);
        }
    }

    private Properties loadBundle(String bundleName) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(bundleName)) {
            assertTrue(inputStream != null, "Missing bundle: " + bundleName);
            properties.load(inputStream);
        }
        return properties;
    }

    private TreeSet<String> menuKeys(Properties properties) {
        TreeSet<String> keys = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("i18n_menu.")) {
                keys.add(key);
            }
        }
        return keys;
    }
}
