package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies durability damage to an item.
 */
@FunctionalInterface
public interface DamageItemHandler {

    /**
     * @param itemStack the item to damage
     * @param damage the durability damage
     * @param owner the entity holding the item
     * @return the resulting item
     */
    ItemStack execute(ItemStack itemStack, int damage, Entity owner);
}
