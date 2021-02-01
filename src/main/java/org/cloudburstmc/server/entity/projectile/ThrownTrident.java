package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.Projectile;

import javax.annotation.Nonnull;

public interface ThrownTrident extends Projectile {

    ItemStack getTrident();

    void setTrident(@Nonnull ItemStack trident);
}
