package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;

import javax.annotation.Nonnull;

public interface ThrownTrident extends Projectile {

    ItemStack getTrident();

    void setTrident(@Nonnull ItemStack trident);
}
