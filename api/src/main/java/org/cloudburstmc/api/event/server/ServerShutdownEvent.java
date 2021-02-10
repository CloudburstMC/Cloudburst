package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerShutdownEvent extends Event {
    public static final ServerShutdownEvent INSTANCE = new ServerShutdownEvent();
}
