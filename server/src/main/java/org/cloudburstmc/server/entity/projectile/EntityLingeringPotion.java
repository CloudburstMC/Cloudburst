package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.LingeringPotion;
import org.cloudburstmc.api.event.entity.LingeringPotionSplashEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.misc.EntityAreaEffectCloud;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.LINGERING;

public class EntityLingeringPotion extends EntitySplashPotion implements LingeringPotion {

    public EntityLingeringPotion(EntityType<LingeringPotion> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.data.setFlag(LINGERING, true);
    }

    @Override
    protected void applyPotionImpact(Entity collidedWith) {
        Location location = this.getLocation();
        EntityAreaEffectCloud entity = (EntityAreaEffectCloud) CloudEntityRegistry.get().newEntity(EntityTypes.AREA_EFFECT_CLOUD, location);
        entity.setOwner(this.getOwner());
        entity.setPotionType(this.getPotionType());
        entity.setPotionDurationScale(0.25F);
        entity.setRadius(3.0F);
        entity.setRadiusOnUse(-0.5F);
        entity.setDuration(600);
        entity.setWaitTime(10);
        entity.setReapplicationDelay(20);
        entity.setRadiusPerTick(-entity.getRadius() / entity.getDuration());

        LingeringPotionSplashEvent event = new LingeringPotionSplashEvent(this, entity);
        this.server.getEventManager().fire(event);
        if (!event.isCancelled()) {
            entity.spawnToAll();
        }
    }
}
