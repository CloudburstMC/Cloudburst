package org.cloudburstmc.api.event;

/**
 * A notification dispatched to registered listeners on the calling thread.
 * Listeners registered for a base event type also receive its asynchronous subclasses.
 * Such listeners may be invoked concurrently and must follow each event's threading contract.
 */
public abstract class Event {

    private final boolean asynchronous;

    private boolean isCancelled = false;

    /**
     * Creates a server-thread event.
     */
    protected Event() {
        this(false);
    }

    /**
     * Creates an event with the specified threading contract.
     * This flag does not schedule or move listener execution.
     *
     * @param asynchronous whether listeners run outside the server thread
     */
    protected Event(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /**
     * Returns whether this event is dispatched outside the server thread.
     * Asynchronous listeners must not access mutable world or player state.
     *
     * @return whether listener execution is asynchronous
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

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
