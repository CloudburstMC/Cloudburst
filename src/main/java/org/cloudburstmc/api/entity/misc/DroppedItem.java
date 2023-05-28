package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

public interface DroppedItem extends Entity {

    ItemStack getItem();

    void setItem(@NonNull ItemStack item);

    @NonNegative
    int getPickupDelay();

    void setPickupDelay(@NonNegative int pickupDelay);
}
