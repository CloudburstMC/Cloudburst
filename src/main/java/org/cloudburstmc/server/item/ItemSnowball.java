package org.cloudburstmc.server.item;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemSnowball extends ProjectileItem {

    public ItemSnowball(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public EntityType<? extends Projectile> getProjectileEntityType() {
        return EntityTypes.SNOWBALL;
    }

    @Override
    public float getThrowForce() {
        return 1.5f;
    }
}
