package org.cloudburstmc.server.level;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LevelClock {

    private long totalTicks;
    private float partialTick;
    private float rate = 1;
    @Setter
    private boolean paused;

    public void setTotalTicks(long totalTicks) {
        this.totalTicks = totalTicks;
        this.partialTick = 0;
    }

    public void setPartialTick(float partialTick) {
        if (!Float.isFinite(partialTick) || partialTick < 0 || partialTick >= 1) {
            throw new IllegalArgumentException("Partial clock tick must be in [0, 1)");
        }

        this.partialTick = partialTick;
    }

    public void setRate(float rate) {
        if (!Float.isFinite(rate) || rate < 0.00001f || rate > 1000) {
            throw new IllegalArgumentException("Clock rate must be between 0.00001 and 1000");
        }

        this.rate = rate;
    }

    public void tick() {
        if (this.paused) {
            return;
        }

        float next = this.partialTick + this.rate;
        int wholeTicks = (int) Math.floor(next);
        this.totalTicks = Math.addExact(this.totalTicks, wholeTicks);
        this.partialTick = next - wholeTicks;
    }

    public LevelClock copy() {
        LevelClock copy = new LevelClock();
        copy.totalTicks = this.totalTicks;
        copy.partialTick = this.partialTick;
        copy.rate = this.rate;
        copy.paused = this.paused;
        return copy;
    }
}
