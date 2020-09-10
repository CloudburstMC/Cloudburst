package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.entity.projectile.LingeringPotion;
import org.cloudburstmc.server.item.ItemStack;

public class ItemPotionLingering extends ProjectileItemBehavior {

    public ItemPotionLingering() {
        super(EntityTypes.LINGERING_POTION, 0.5f);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    protected void onProjectileCreation(ItemStack item, Projectile projectile) {
        ((LingeringPotion) projectile).setPotionId(this.getMeta());
    }
}
