package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/25 by xtypr.
 * Package cn.nukkit.item in project Nukkit .
 */
public class ItemExpBottle extends ProjectileItem {

    public ItemExpBottle(Identifier id) {
        super(id);
    }

    @Override
    public EntityType<? extends Projectile> getProjectileEntityType() {
        return EntityTypes.XP_BOTTLE;
    }

    @Override
    public float getThrowForce() {
        return 1f;
    }

}
