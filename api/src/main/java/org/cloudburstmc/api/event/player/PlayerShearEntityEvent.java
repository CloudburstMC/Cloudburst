package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * Called when a player shears an entity.
 */
public final class PlayerShearEntityEvent extends PlayerEvent implements Cancellable {

    private final Entity entity;
    private final ItemStack item;
    private List<ItemStack> drops;

    /**
     * Creates an entity-shear event.
     *
     * @param player the player shearing the entity
     * @param entity the entity being sheared
     * @param item the item used to shear the entity
     * @param drops the items that will be dropped
     */
    public PlayerShearEntityEvent(Player player, Entity entity, ItemStack item, List<ItemStack> drops) {
        super(player);
        this.entity = entity;
        this.item = item;
        this.drops = List.copyOf(drops);
    }

    /**
     * Returns the entity being sheared.
     *
     * @return the entity being sheared
     */
    public Entity getEntity() {
        return this.entity;
    }

    /**
     * Returns the item used to shear the entity.
     *
     * @return the shearing item
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Returns the items that will be dropped.
     *
     * @return an immutable list of drops
     */
    public List<ItemStack> getDrops() {
        return this.drops;
    }

    /**
     * Sets the items that will be dropped.
     *
     * @param drops the new drops
     */
    public void setDrops(List<ItemStack> drops) {
        this.drops = List.copyOf(drops);
    }
}
