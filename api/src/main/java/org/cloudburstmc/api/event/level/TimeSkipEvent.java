package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Level;

import java.util.Objects;

/**
 * Called before daylight time jumps. Cancellation leaves the time unchanged.
 * Ordinary clock ticks do not call this event.
 * Cancelling a night skip leaves the sleeping players in bed.
 */
public class TimeSkipEvent extends LevelEvent implements Cancellable {

    private final TimeSkipReason skipReason;
    private long skipAmount;

    /**
     * @param level      the affected level
     * @param skipReason the cause of the change
     * @param skipAmount the signed number of daylight ticks to skip
     */
    public TimeSkipEvent(Level level, TimeSkipReason skipReason, long skipAmount) {
        super(Objects.requireNonNull(level, "level"));
        this.skipReason = Objects.requireNonNull(skipReason, "skipReason");
        this.skipAmount = skipAmount;
    }

    /**
     * @return the cause of the change
     */
    public TimeSkipReason getSkipReason() {
        return this.skipReason;
    }

    /**
     * @return the signed number of daylight ticks to skip
     */
    public long getSkipAmount() {
        return this.skipAmount;
    }

    /**
     * Changes the requested jump without changing elapsed game time.
     *
     * @param skipAmount the signed number of daylight ticks to skip
     */
    public void setSkipAmount(long skipAmount) {
        this.skipAmount = skipAmount;
    }
}
