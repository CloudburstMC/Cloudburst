package org.cloudburstmc.server.event.player;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * Created by CreeperFace on 1. 1. 2017.
 */
public class PlayerInteractEntityEvent extends PlayerEvent implements Cancellable {

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
