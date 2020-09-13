package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.entity.projectile.LingeringPotion;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.potion.Potion;

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
