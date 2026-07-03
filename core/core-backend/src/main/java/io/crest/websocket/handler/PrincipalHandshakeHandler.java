package io.crest.websocket.handler;

import io.crest.auth.bo.TokenUserBO;
import io.crest.utils.AuthUtils;
import io.crest.websocket.entity.CrestPrincipal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

// 定义实时消息通道的配置入口
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            TokenUserBO user = AuthUtils.getUser();
            if (user == null || user.getUserId() == null) {
                return null;
            }
            return new CrestPrincipal(String.valueOf(user.getUserId()));
        }
        return null;
    }
}
