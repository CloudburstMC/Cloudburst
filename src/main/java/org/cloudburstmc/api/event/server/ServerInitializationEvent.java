package org.cloudburstmc.api.event.server;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.event.Event;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServerInitializationEvent extends Event {
    public static final ServerInitializationEvent INSTANCE = new ServerInitializationEvent();
}