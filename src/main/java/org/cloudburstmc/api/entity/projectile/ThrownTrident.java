package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;

public interface ThrownTrident extends Projectile {

    ItemStack getTrident();

    void setTrident(@NonNull ItemStack trident);
}
