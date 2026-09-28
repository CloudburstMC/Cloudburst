package org.cloudburstmc.server.entity.ai.sensor;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.memory.MemoryTypes;
import org.cloudburstmc.api.entity.ai.sensor.Sensor;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.passive.Animal;

/**
 * Scans for the nearest player holding a breeding item for this animal.
 *
 * @author daoge_cmd
 */
public class NearestFeedingPlayerSensor implements Sensor {

    protected final double range;
    protected final int period;

    public NearestFeedingPlayerSensor(double range, int period) {
        this.range = range;
        this.period = period;
    }

    public NearestFeedingPlayerSensor(double range) {
        this(range, 1);
    }

    @Override
    public void sense(EntityIntelligent entity) {
        if (!(entity instanceof Animal animal)) {
            return;
        }

        var location = entity.getLocation();
        double rangeSq = range * range;

        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (var player : entity.getLevel().getPlayers().values()) {
            if (player == null || !player.isAlive()) continue;

            double distSq = distanceSquared(location, player);
            if (distSq > rangeSq) continue;

            var itemInHand = player.getInventory().getSelectedItem();
            if (itemInHand == null || !animal.isBreedingItem(itemInHand)) continue;

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }

        entity.getMemoryStorage().put(MemoryTypes.NEAREST_FEEDING_PLAYER, nearest != null ? nearest.getUniqueId() : null);
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
