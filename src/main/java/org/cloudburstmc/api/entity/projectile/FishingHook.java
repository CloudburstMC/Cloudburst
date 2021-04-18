package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;

public interface FishingHook extends Projectile {

    @Nullable
    ItemStack getRod();

    void setRod(@Nullable ItemStack rod);

    void reelLine();
}
