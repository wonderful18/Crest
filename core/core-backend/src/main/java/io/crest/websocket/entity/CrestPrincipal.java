package io.crest.websocket.entity;

import java.security.Principal;

public class CrestPrincipal implements Principal {

    public CrestPrincipal(String name) {
        this.name = name;
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }
}
