package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.server.entity.projectile.EntitySplashPotion;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.POTION_AUX_VALUE;

/**
 * Created on 2015/12/27 by xtypr.
 * Package cn.nukkit.item in project Nukkit .
 */
public class ItemPotionSplashBehavior extends ItemProjectileBehavior {

    public ItemPotionSplashBehavior() {
        super(EntityTypes.SPLASH_POTION, 0.5f);
    }

    @Override
    protected void onProjectileCreation(ItemStack item, Projectile entity) {
        ((EntitySplashPotion) entity).getData().setShort(POTION_AUX_VALUE, item.getMetadata(Potion.class).getNetworkId());
    }
}
