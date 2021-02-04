package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.item.ItemStack;

public interface Campfire extends BlockEntity {

    boolean putItemInFire(ItemStack item);

    default boolean putItemInFire(ItemStack item, int index) {
        return putItemInFire(item, index, false);
    }

    boolean putItemInFire(ItemStack item, int index, boolean overwrite);
}
