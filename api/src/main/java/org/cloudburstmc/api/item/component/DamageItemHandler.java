package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies durability damage to an item stack.
 */
@FunctionalInterface
public interface DamageItemHandler {

    /**
     * @param itemStack the item to damage
     * @param damage the durability damage
     * @param owner the entity holding the item
     * @return the resulting item, or {@link ItemStack#EMPTY} if the item broke
     */
    ItemStack execute(ItemStack itemStack, int damage, Entity owner);
}
