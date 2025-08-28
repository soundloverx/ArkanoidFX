package org.overb.arkanoidfx.game.core;

import org.overb.arkanoidfx.enums.EventType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

public final class EventBus {

    private static final EnumMap<EventType, List<Consumer<GameEvent>>> listeners = new EnumMap<>(EventType.class);

    private EventBus() {
    }

    public static synchronized void subscribe(EventType type, Consumer<GameEvent> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public static synchronized void unsubscribe(EventType type, Consumer<GameEvent> listener) {
        List<Consumer<GameEvent>> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                listeners.remove(type);
            }
        }
    }

    public static void publish(GameEvent event) {
        List<Consumer<GameEvent>> snapshot;
        synchronized (EventBus.class) {
            List<Consumer<GameEvent>> list = listeners.get(event.type());
            if (list == null || list.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(list);
        }
        for (Consumer<GameEvent> c : snapshot) {
            try {
                c.accept(event);
            } catch (Exception ignore) {
            }
        }
    }
}