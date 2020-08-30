package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.utils.Identifier;

public class ItemEnderPearl extends ProjectileItem {

    public ItemEnderPearl(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public EntityType<? extends Projectile> getProjectileEntityType() {
        return EntityTypes.ENDER_PEARL;
    }

    @Override
    public float getThrowForce() {
        return 1.5f;
    }
}
