package io.crest.extensions.datafilling.plugin;

import io.crest.exception.CrestException;
import io.crest.extensions.datafilling.factory.ExtDDLProviderFactory;
import io.crest.extensions.datafilling.provider.ExtDDLProvider;
import io.crest.extensions.datafilling.vo.PluginDataFillingVO;
import io.crest.plugins.template.CrestPlugin;
import io.crest.plugins.vo.CrestPluginVO;
import io.crest.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;

public abstract class DataFillingPlugin extends ExtDDLProvider implements CrestPlugin {

    @Override
    public void loadPlugin() {
        PluginDataFillingVO viewConfig = getConfig();
        ExtDDLProviderFactory.loadPlugin(viewConfig.getType(), this);
    }


    public PluginDataFillingVO getConfig() {
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
        if (StringUtils.isBlank(config)) {
            CrestException.throwException("Plugin config not exists");
            return null;
        }
        PluginDataFillingVO vo = JsonUtil.parseObject(config, PluginDataFillingVO.class);
        if (vo == null) {
            CrestException.throwException("Plugin config not exists");
            return null;
        }
        vo.setIcon(pluginInfo.getIcon());
        return vo;
    }

    @Override
    public void unloadPlugin() {

    }
}
