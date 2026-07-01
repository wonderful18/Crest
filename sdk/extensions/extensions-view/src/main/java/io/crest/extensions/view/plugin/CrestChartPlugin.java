package io.crest.extensions.view.plugin;

import io.crest.exception.CrestException;
import io.crest.extensions.view.factory.PluginsChartFactory;
import io.crest.extensions.view.vo.PluginViewVO;
import io.crest.plugins.template.CrestPlugin;
import io.crest.plugins.vo.CrestPluginVO;
import io.crest.utils.JsonUtil;

public abstract class CrestChartPlugin extends AbstractChartPlugin implements CrestPlugin {

    @Override
    public void loadPlugin() {
        PluginViewVO viewConfig = getConfig();
        PluginsChartFactory.loadPlugin(viewConfig.getRender(), viewConfig.getChartValue(), this);
    }

    public PluginViewVO getConfig() {
        CrestPluginVO pluginInfo = null;
        try {
            pluginInfo = getPluginInfo();
        } catch (Exception e) {
            CrestException.throwException(e);
            return null;
        }
        if (pluginInfo == null) {
            CrestException.throwException("Plugin info not exists");
            return null;
        }
        String config = pluginInfo.getConfig();
        PluginViewVO vo = JsonUtil.parseObject(config, PluginViewVO.class);
        if (vo == null) {
            CrestException.throwException("Plugin config not exists");
            return null;
        }
        vo.setIcon(pluginInfo.getIcon());
        return vo;
    }
}
