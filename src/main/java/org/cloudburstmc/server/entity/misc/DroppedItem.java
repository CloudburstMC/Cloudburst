package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public interface DroppedItem extends Entity {

    ItemStack getItem();

    void setItem(@Nonnull ItemStack item);

    @Nonnegative
    int getPickupDelay();

    void setPickupDelay(@Nonnegative int pickupDelay);
}
