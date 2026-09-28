package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called when a player picks an entity with an item representation.
 */
public class PlayerPickEntityEvent extends PlayerPickItemEvent {
    private final Entity entity;

    /**
     * Creates an entity pick action before its inventory changes are applied.
     *
     * @param player      the player performing the pick action
     * @param entity      the entity being picked
     * @param item        the resolved item
     * @param includeData whether supported entity data was requested in creative mode
     * @param targetSlot  the destination hotbar slot, from 0 to 8
     * @param sourceSlot  the source inventory slot, from 0 to 35, or -1 to create the item
     */
    public PlayerPickEntityEvent(Player player, Entity entity, ItemStack item, boolean includeData, int targetSlot, int sourceSlot) {
        super(player, item, includeData, targetSlot, sourceSlot);
        this.entity = Objects.requireNonNull(entity, "entity");
    }

    /**
     * Returns the entity being picked.
     *
     * @return the picked entity
     */
    public Entity getEntity() {
        return this.entity;
    }
}
