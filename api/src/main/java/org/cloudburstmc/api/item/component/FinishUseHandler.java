package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies an item's behavior after its use duration completes.
 */
@FunctionalInterface
public interface FinishUseHandler {

    /**
     * @param itemStack the item being used
     * @param entity    the entity using the item
     * @return the resulting item stack
     */
    ItemStack execute(ItemStack itemStack, Entity entity);
}
