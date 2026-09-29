package org.cloudburstmc.server.entity.ai.sensor;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.entity.ai.sensor.Sensor;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

/**
 * Scans for the nearest player within range and stores it in memory.
 *
 * @author daoge_cmd
 */
public class NearestPlayerSensor implements Sensor {

    protected final double range;
    protected final double minRange;
    protected final int period;

    public NearestPlayerSensor(double range, double minRange, int period) {
        this.range = range;
        this.minRange = minRange;
        this.period = period;
    }

    public NearestPlayerSensor(double range) {
        this(range, 0, 1);
    }

    @Override
    public void sense(EntityIntelligent entity) {
        var location = entity.getLocation();
        double rangeSq = range * range;
        double minRangeSq = minRange * minRange;

        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (var player : entity.getLevel().getPlayers().values()) {
            if (player == null || !player.isAlive()) continue;

            double distSq = distanceSquared(location, player);
            if (distSq < minRangeSq || distSq > rangeSq) continue;

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }

        entity.getMemoryStorage().put(MemoryTypes.NEAREST_PLAYER, nearest != null ? nearest.getUniqueId() : null);
    }

    @Override
    public int getPeriod() {
        return period;
    }

    private static double distanceSquared(Location location, Entity entity) {
        double x = entity.getX() - location.getX();
        double y = entity.getY() - location.getY();
        double z = entity.getZ() - location.getZ();
        return x * x + y * y + z * z;
    }
}
