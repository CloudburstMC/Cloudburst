package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemEgg extends ProjectileItem {

    public ItemEgg(Identifier id) {
        super(id);
    }

    @Override
    public EntityType<? extends Projectile> getProjectileEntityType() {
        return EntityTypes.EGG;
    }

    @Override
    public float getThrowForce() {
        return 1.5f;
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }
}
