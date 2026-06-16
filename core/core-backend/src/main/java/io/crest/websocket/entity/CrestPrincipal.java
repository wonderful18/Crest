package io.crest.websocket.entity;

import java.security.Principal;

// 定义实时消息通道的配置入口
public class CrestPrincipal implements Principal {

    public CrestPrincipal(String name) {
        this.name = name;
    }

    private String name;

    @Override
    // 执行当前服务方法并返回处理结果
    public String getName() {
        return name;
    }
}
