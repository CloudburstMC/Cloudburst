package org.cloudburstmc.api.event.server;

/**
 * Fired on the server thread after a simulation tick finishes and before waiting for the next tick.
 * A tick that fails does not fire this event.
 */
public class ServerTickEndEvent extends ServerEvent {

    private final int tickNumber;
    private final double tickDuration;
    private final long deadlineNanos;

    /**
     * Creates a tick-end notification.
     *
     * @param tickNumber    number of the completed tick, with the first tick numbered one
     * @param tickDuration  active tick duration in milliseconds
     * @param timeRemaining nanoseconds until the next scheduled tick, which may be negative
     * @throws IllegalArgumentException if the duration is negative or not finite
     */
    public ServerTickEndEvent(int tickNumber, double tickDuration, long timeRemaining) {
        if (!Double.isFinite(tickDuration) || tickDuration < 0) {
            throw new IllegalArgumentException("Tick duration must be finite and nonnegative");
        }

        this.tickNumber = tickNumber;
        this.tickDuration = tickDuration;
        this.deadlineNanos = System.nanoTime() + timeRemaining;
    }

    /**
     * Returns the number of the completed tick.
     *
     * @return the tick number, starting at one
     */
    public int getTickNumber() {
        return this.tickNumber;
    }

    /**
     * Returns active tick duration measured before this event's listeners run.
     * Time spent waiting for the next tick is excluded.
     *
     * @return the tick duration in milliseconds
     */
    public double getTickDuration() {
        return this.tickDuration;
    }

    /**
     * Returns the current time budget before the next scheduled tick.
     * The value decreases as listeners run and is negative when the deadline has passed.
     *
     * @return remaining nanoseconds, including negative values for overdue ticks
     */
    public long getTimeRemaining() {
        return this.deadlineNanos - System.nanoTime();
    }
}
