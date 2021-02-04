package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public interface DroppedItem extends Entity {

    ItemStack getItem();

    void setItem(@Nonnull ItemStack item);

    @Nonnegative
    int getPickupDelay();

    void setPickupDelay(@Nonnegative int pickupDelay);
}
