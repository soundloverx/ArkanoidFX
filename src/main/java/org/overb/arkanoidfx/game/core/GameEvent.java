package org.overb.arkanoidfx.game.core;

import org.overb.arkanoidfx.enums.EventType;

public record GameEvent(EventType type, Object payload) {

    public static GameEvent of(EventType type) {
        return new GameEvent(type, null);
    }

    public static GameEvent of(EventType type, Object payload) {
        return new GameEvent(type, payload);
    }
}
