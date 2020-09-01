package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.behavior.Item;

import javax.annotation.Nonnull;

public interface ThrownTrident extends Projectile {

    Item getTrident();

    void setTrident(@Nonnull Item trident);
}
