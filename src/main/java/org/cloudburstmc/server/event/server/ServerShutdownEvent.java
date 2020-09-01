package org.cloudburstmc.server.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.event.Event;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerShutdownEvent extends Event {
    public static final ServerShutdownEvent INSTANCE = new ServerShutdownEvent();
}
