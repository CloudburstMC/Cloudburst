package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;

import javax.annotation.Nullable;

public interface FishingHook extends Projectile {

    @Nullable
    ItemStack getRod();

    void setRod(@Nullable ItemStack rod);

    void reelLine();
}
