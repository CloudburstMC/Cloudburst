package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.player.Player;

public final class PlayerMouseOverEntityEvent extends PlayerEvent {

    private final Entity entity;

    public PlayerMouseOverEntityEvent(Player player, Entity entity) {
        super(player);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
