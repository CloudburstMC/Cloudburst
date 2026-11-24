package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.LlamaSpit;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudLivingEntity;

public class EntityLlamaSpit extends EntityProjectile implements LlamaSpit {

    public EntityLlamaSpit(EntityType<LlamaSpit> type, Location location) {
        super(type, location);
    }

    @Override
    protected float getBaseDamage() {
        return 1f;
    }

    @Override
    public int getResultDamage() {
        return (int) Math.ceil(this.getDamage());
    }

    @Override
    protected void onCollideWithEntity(Entity target) {
        if (this.getOwner() instanceof CloudLivingEntity) {
            this.damageEntity(target);
        }
        this.remove(EntityRemoveCause.HIT);
    }

    @Override
    protected void afterHit() {
        this.remove(EntityRemoveCause.HIT);
    }
}
