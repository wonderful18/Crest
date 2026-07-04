package io.crest.security;

import io.crest.exception.CrestException;
import io.crest.extensions.datasource.plugin.CrestDatasourcePlugin;
import io.crest.extensions.view.plugin.CrestChartPlugin;
import io.crest.plugins.vo.CrestPluginVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginConfigSecurityTest {

    @Test
    void datasourcePluginRejectsBlankConfig() throws Exception {
        CrestDatasourcePlugin plugin = mock(CrestDatasourcePlugin.class, CALLS_REAL_METHODS);
        when(plugin.getPluginInfo()).thenReturn(pluginInfo(""));

        CrestException exception = assertThrows(CrestException.class, plugin::getConfig);

        assertEquals("Plugin config not exists", exception.getMsg());
    }

    @Test
    void chartPluginRejectsBlankConfig() throws Exception {
        CrestChartPlugin plugin = mock(CrestChartPlugin.class, CALLS_REAL_METHODS);
        when(plugin.getPluginInfo()).thenReturn(pluginInfo(" "));

        CrestException exception = assertThrows(CrestException.class, plugin::getConfig);

        assertEquals("Plugin config not exists", exception.getMsg());
    }

    private CrestPluginVO pluginInfo(String config) {
        CrestPluginVO pluginInfo = new CrestPluginVO();
        pluginInfo.setConfig(config);
        return pluginInfo;
    }
}
