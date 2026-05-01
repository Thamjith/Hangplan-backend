package com.hangplan.realtime;

import java.security.Principal;
import java.util.UUID;

public record HangplanStompPrincipal(UUID userId, String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
