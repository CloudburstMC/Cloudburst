package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies feedback when an equipped or held item runs out of durability.
 */
@FunctionalInterface
public interface ItemBreakHandler {

    /**
     * Runs before the broken stack is removed from its slot.
     *
     * @param item  item that broke
     * @param owner entity whose item broke
     */
    void execute(ItemStack item, Entity owner);
}
