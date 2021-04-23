package org.cloudburstmc.server;


import org.cloudburstmc.server.math.NukkitMath;

/**
 * Tick statistics calculation object, handles TPS and tick usage calculation
 */
public class TickStatistics {


    private final float[] tickAverage = {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20};

    private final float[] useAverage = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private float maxTick = 20;

    private float maxUse = 0;


    /**
     * update statistics according to completed tick time consumption
     * @param beforeTickNano nanosecond timestamp before ticking
     * @param afterTickNano nanosecond timestamp after ticking
     */
    public void onTickCompleted(long beforeTickNano, long afterTickNano) {
        float tick = (float) Math.min(20, 1000000000 / Math.max(1000000, ((double) afterTickNano - beforeTickNano)));
        float use = (float) Math.min(1, ((double) (afterTickNano - beforeTickNano)) / 50000000);

        if (this.maxTick > tick) {
            this.maxTick = tick;
        }

        if (this.maxUse < use) {
            this.maxUse = use;
        }

        System.arraycopy(this.tickAverage, 1, this.tickAverage, 0, this.tickAverage.length - 1);
        this.tickAverage[this.tickAverage.length - 1] = tick;

        System.arraycopy(this.useAverage, 1, this.useAverage, 0, this.useAverage.length - 1);
        this.useAverage[this.useAverage.length - 1] = use;
    }

    /**
     * reset the values used for computing current TPS / tick usage
     */
    public void resetCurrent() {
        this.maxTick = 20;
        this.maxUse = 0;
    }

    public float getTicksPerSecond() {
        return this.maxTick;
    }

    public float getReadableTicksPerSecond() {
        return ((float) Math.round(this.maxTick * 100)) / 100;
    }

    public float getTicksPerSecondAverage() {
        float sum = 0;
        int count = this.tickAverage.length;
        for (float aTickAverage : this.tickAverage) {
            sum += aTickAverage;
        }
        return (float) NukkitMath.round(sum / count, 2);
    }

    public float getTickUsage() {
        return this.maxUse;
    }

    public float getReadableTickUsage() {
        return (float) NukkitMath.round(this.maxUse * 100, 2);
    }

    public float getTickUsageAverage() {
        float sum = 0;
        int count = this.useAverage.length;
        for (float aUseAverage : this.useAverage) {
            sum += aUseAverage;
        }
        return ((float) Math.round(sum / count * 100)) / 100;
    }

}
