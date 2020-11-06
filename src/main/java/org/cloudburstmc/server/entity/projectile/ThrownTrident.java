package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.ItemStack;

import javax.annotation.Nonnull;

public interface ThrownTrident extends Projectile {

    ItemStack getTrident();

    void setTrident(@Nonnull ItemStack trident);
}
