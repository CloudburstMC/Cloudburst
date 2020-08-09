package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.Item;

import javax.annotation.Nullable;

public interface FishingHook extends Projectile {

    @Nullable
    Item getRod();

    void setRod(@Nullable Item rod);

    void reelLine();
}
