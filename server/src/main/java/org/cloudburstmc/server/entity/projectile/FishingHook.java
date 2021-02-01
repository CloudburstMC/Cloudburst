package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.Projectile;

import javax.annotation.Nullable;

public interface FishingHook extends Projectile {

    @Nullable
    ItemStack getRod();

    void setRod(@Nullable ItemStack rod);

    void reelLine();
}
