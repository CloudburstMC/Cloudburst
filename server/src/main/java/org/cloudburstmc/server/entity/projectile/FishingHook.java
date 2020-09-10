package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.ItemStack;

import javax.annotation.Nullable;

public interface FishingHook extends Projectile {

    @Nullable
    ItemStack getRod();

    void setRod(@Nullable ItemStack rod);

    void reelLine();
}
