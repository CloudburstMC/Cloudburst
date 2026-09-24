package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Runs once per tick while an entity is actively using an item.
 */
@FunctionalInterface
public interface UseTickHandler {

    /**
     * @param item      the item being used
     * @param entity    the entity using the item
     * @param ticksUsed the number of server ticks spent using the item
     * @return the item and whether active use should end
     */
    UseTickResult execute(ItemStack item, Entity entity, int ticksUsed);
}
