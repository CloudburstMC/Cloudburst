package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.server.item.behavior.Item;

public interface Campfire extends BlockEntity {

    boolean putItemInFire(Item item);

    default boolean putItemInFire(Item item, int index) {
        return putItemInFire(item, index, false);
    }

    boolean putItemInFire(Item item, int index, boolean overwrite);
}
