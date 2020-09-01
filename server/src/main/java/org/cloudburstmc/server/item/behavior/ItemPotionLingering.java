package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.Projectile;
import org.cloudburstmc.server.entity.projectile.LingeringPotion;
import org.cloudburstmc.server.utils.Identifier;

public class ItemPotionLingering extends ProjectileItem {

    public ItemPotionLingering(Identifier id) {
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
        return EntityTypes.LINGERING_POTION;
    }

    @Override
    public float getThrowForce() {
        return 0.5f;
    }

    @Override
    protected void onProjectileCreation(Projectile projectile) {
        ((LingeringPotion) projectile).setPotionId(this.getMeta());
    }
}
