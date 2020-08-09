package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.Item;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public interface DroppedItem extends Entity {

    Item getItem();

    void setItem(@Nonnull Item item);

    @Nonnegative
    int getPickupDelay();

    void setPickupDelay(@Nonnegative int pickupDelay);
}
