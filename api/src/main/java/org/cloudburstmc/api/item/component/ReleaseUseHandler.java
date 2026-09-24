package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies an item's behavior when its holder stops using it.
 */
@FunctionalInterface
public interface ReleaseUseHandler {

    /**
     * @param itemStack the item being released
     * @param entity    the entity using the item
     * @param ticksUsed the number of server ticks spent using the item
     * @return the resulting item stack
     */
    ItemStack execute(ItemStack itemStack, Entity entity, int ticksUsed);
}
