package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.entity.projectile.LingeringPotion;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.Potion;

public class ItemPotionLingeringBehavior extends ItemProjectileBehavior {

    public ItemPotionLingeringBehavior() {
        super(EntityTypes.LINGERING_POTION, 0.5f);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    protected void onProjectileCreation(ItemStack item, Projectile projectile) {
        ((LingeringPotion) projectile).setPotionId(item.getMetadata(Potion.class).getId());
    }
}
