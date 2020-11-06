package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.ItemStack;

public interface Campfire extends BlockEntity {

    boolean putItemInFire(ItemStack item);

    default boolean putItemInFire(ItemStack item, int index) {
        return putItemInFire(item, index, false);
    }

    boolean putItemInFire(ItemStack item, int index, boolean overwrite);
}
