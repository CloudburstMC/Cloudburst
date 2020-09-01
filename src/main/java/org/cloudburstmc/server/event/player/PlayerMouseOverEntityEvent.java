package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.player.Player;

public class PlayerMouseOverEntityEvent extends PlayerEvent {

    private final Entity entity;

    public PlayerMouseOverEntityEvent(Player player, Entity entity) {
        super(player);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
