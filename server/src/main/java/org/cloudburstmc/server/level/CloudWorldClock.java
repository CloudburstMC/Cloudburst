package org.cloudburstmc.server.level;

import lombok.Getter;
import org.cloudburstmc.api.event.level.TimeSkipEvent;
import org.cloudburstmc.api.event.level.TimeSkipReason;
import org.cloudburstmc.api.level.WorldClock;

public class CloudWorldClock implements WorldClock {

    @Getter
    private final CloudLevel level;
    private final LevelClock state;
    private boolean changingTime;

    public CloudWorldClock(CloudLevel level, LevelClock state) {
        this.level = level;
        this.state = state;
    }

    @Override
    public long getTotalTicks() {
        return this.state.getTotalTicks();
    }

    @Override
    public boolean setTotalTicks(long ticks) {
        return this.skipTicks(Math.subtractExact(ticks, this.getTotalTicks()), TimeSkipReason.CUSTOM);
    }

    public boolean skipTicks(long ticks, TimeSkipReason reason) {
        if (this.changingTime) {
            return false;
        }

        long previous = this.getTotalTicks();
        TimeSkipEvent event = new TimeSkipEvent(this, reason, ticks);
        this.changingTime = true;
        try {
            this.level.getServer().getEventManager().fire(event);
        } finally {
            this.changingTime = false;
        }

        if (event.isCancelled()) {
            return false;
        }

        long next = Math.addExact(previous, event.getSkipAmount());
        if (previous != next) {
            this.state.setTotalTicks(next);
            this.level.synchronizeDaylightTime();
        }

        return true;
    }

    @Override
    public boolean isPaused() {
        return this.state.isPaused();
    }

    @Override
    public void setPaused(boolean paused) {
        if (paused != this.state.isPaused()) {
            this.state.setPaused(paused);
            this.level.synchronizeDaylightSettings();
        }
    }

    @Override
    public float getRate() {
        return this.state.getRate();
    }

    @Override
    public void setRate(float rate) {
        if (rate != this.state.getRate()) {
            this.state.setRate(rate);
            this.level.synchronizeDaylightSettings();
        }
    }
}
