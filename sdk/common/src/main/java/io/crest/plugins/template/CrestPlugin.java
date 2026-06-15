package io.crest.plugins.template;

import io.crest.plugins.vo.CrestPluginVO;

// 定义当前业务对象的核心职责
public interface CrestPlugin {
    CrestPluginVO getPluginInfo() throws Exception;

    void loadPlugin();

    default void unloadPlugin() {
    }
}
