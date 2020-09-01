package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.utils.Identifier;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.POTION_AUX_VALUE;

/**
 * Created on 2015/12/27 by xtypr.
 * Package cn.nukkit.item in project Nukkit .
 */
public class ItemPotionSplash extends ProjectileItem {

    public ItemPotionSplash(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public EntityType<? extends Projectile> getProjectileEntityType() {
        return EntityTypes.SPLASH_POTION;
    }

    @Override
    public float getThrowForce() {
        return 0.5f;
    }

    @Override
    protected void onProjectileCreation(Projectile entity) {
        entity.getData().setShort(POTION_AUX_VALUE, this.getMeta());
    }
}
