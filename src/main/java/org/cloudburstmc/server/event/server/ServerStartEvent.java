package org.cloudburstmc.server.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.event.Event;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerStartEvent extends Event {
    public static final ServerStartEvent INSTANCE = new ServerStartEvent();
}
