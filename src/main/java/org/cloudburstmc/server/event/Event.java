package org.cloudburstmc.server.event;

import org.cloudburstmc.server.utils.EventException;

/**
 * Describes things that happens in the server.
 * <p>
 * Things that happens in the server is called a <b>event</b>. Define a procedure that should be executed
 * when a event happens, this procedure is called a <b>listener</b>.
 * <p>
 * When Nukkit is calling a handler, the event needed to listen is judged by the type of the parameter.
 * <p>
 * For the way to implement a listener, see: {@link Listener}
 */
public abstract class Event {

    private boolean isCancelled = false;

    public boolean isCancelled() {
        if (!(this instanceof Cancellable)) {
            throw new EventException("Event is not Cancellable");
        }
        return isCancelled;
    }

    public void setCancelled() {
        setCancelled(true);
    }

    public void setCancelled(boolean value) {
        if (!(this instanceof Cancellable)) {
            throw new EventException("Event is not Cancellable");
        }
        isCancelled = value;
    }
}
