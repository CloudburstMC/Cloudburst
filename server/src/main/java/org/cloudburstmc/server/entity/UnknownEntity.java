package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.player.CloudPlayer;

public class UnknownEntity extends CloudEntity {
    public UnknownEntity(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        // no-op
    }

    @Override
    public void despawnFrom(CloudPlayer player) {
        // no-op
    }

    @Override
    public void spawnTo(Player player) {

    }

    @Override
    public void despawnFrom(Player player) {

    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        return false;
    }
}
