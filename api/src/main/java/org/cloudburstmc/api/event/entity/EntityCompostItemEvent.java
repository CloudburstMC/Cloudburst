package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.block.CompostItemEvent;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Called when an entity composts an item. Cancelling prevents the attempt and preserves the item.
 */
public class EntityCompostItemEvent extends CompostItemEvent implements Cancellable {

    private final Entity entity;

    public EntityCompostItemEvent(Entity entity, Block composter, ItemStack item, int levelsToRaise) {
        super(composter, item, levelsToRaise);
        this.entity = entity;
    }

    /**
     * Returns the entity using the composter.
     */
    public Entity getEntity() {
        return this.entity;
    }
}
