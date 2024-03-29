package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Created by CreeperFace on 1. 1. 2017.
 */
public final class PlayerInteractEntityEvent extends PlayerEvent implements Cancellable {

    protected final Entity entity;
    protected final ItemStack item;
    protected final Vector3f clickedPos;

    public PlayerInteractEntityEvent(Player player, Entity entity, ItemStack item, Vector3f clickedPos) {
        super(player);
        this.entity = entity;
        this.item = item;
        this.clickedPos = clickedPos;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public Vector3f getClickedPos() {
        return clickedPos;
    }
}
