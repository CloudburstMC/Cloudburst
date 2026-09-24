package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Handles a piercing weapon's separate stab action.
 */
@FunctionalInterface
public interface StabHandler {

    /**
     * @param item   the held piercing weapon
     * @param holder the entity performing the stab
     */
    void execute(ItemStack item, Entity holder);
}
