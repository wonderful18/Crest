package io.crest.websocket.factory;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

public class CrestWebSocketHandlerFactory implements WebSocketHandlerDecoratorFactory {


    @Override
    public WebSocketHandler decorate(WebSocketHandler webSocketHandler) {
        return new CrestWebSocketHandlerDecorator(webSocketHandler);
    }
}
