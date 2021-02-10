package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerStartEvent extends Event {
    public static final ServerStartEvent INSTANCE = new ServerStartEvent();
}
