package org.cloudburstmc.server.plugin;

import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.Listener;
import org.cloudburstmc.server.utils.EventException;

/**
 * author: iNevet
 * Nukkit Project
 */
public interface EventExecutor {

    void execute(Listener listener, Event event) throws EventException;
}
