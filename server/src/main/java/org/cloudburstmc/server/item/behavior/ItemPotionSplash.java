package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.item.ItemStack;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.POTION_AUX_VALUE;

/**
 * Created on 2015/12/27 by xtypr.
 * Package cn.nukkit.item in project Nukkit .
 */
public class ItemPotionSplash extends ProjectileItemBehavior {

    public ItemPotionSplash() {
        super(EntityTypes.SPLASH_POTION, 0.5f);
    }

    @Override
    protected void onProjectileCreation(ItemStack item, Projectile entity) {
        entity.getData().setShort(POTION_AUX_VALUE, this.getMeta());
    }
}
