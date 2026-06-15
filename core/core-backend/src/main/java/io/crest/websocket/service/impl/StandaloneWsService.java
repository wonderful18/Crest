package io.crest.websocket.service.impl;

import io.crest.websocket.WsMessage;
import io.crest.websocket.WsService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
// 定义实时消息通道的配置入口
public class StandaloneWsService implements WsService {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    // 执行当前服务方法并返回处理结果
    public void releaseMessage(WsMessage wsMessage){
        if(ObjectUtils.isEmpty(wsMessage) || ObjectUtils.isEmpty(wsMessage.getUserId()) ||  ObjectUtils.isEmpty(wsMessage.getTopic())) return;
        messagingTemplate.convertAndSendToUser(String.valueOf(wsMessage.getUserId()), wsMessage.getTopic(),wsMessage.getData());
    }
}
