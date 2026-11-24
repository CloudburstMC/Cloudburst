package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.api.entity.projectile.SplashPotion;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.event.entity.PotionSplashEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.potion.CloudPotion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EntitySplashPotion extends EntityAbstractThrownPotion implements SplashPotion {

    public EntitySplashPotion(EntityType<SplashPotion> type, Location location) {
        super(type, location);
    }

    @Override
    protected void applyPotionImpact(Entity collidedWith) {
        Map<LivingEntity, Double> affectedEntities = new LinkedHashMap<>();
        DamageSource damageSource = this.createProjectileDamageSource();
        Set<Entity> entities = this.getLevel().getNearbyEntities(this.getBoundingBox().inflate(4.125f, 2.125f, 4.125f));
        for (Entity candidate : entities) {
            if (candidate instanceof LivingEntity living && !(candidate instanceof ArmorStand)) {
                double distanceSquared = candidate.getPosition().distanceSquared(this.getPosition());
                if (distanceSquared < 16) {
                    double intensity = candidate.equals(collidedWith) ? 1 : 1 - Math.sqrt(distanceSquared) / 4;
                    affectedEntities.put(living, intensity);
                }
            }
        }

        PotionSplashEvent event = new PotionSplashEvent(this, affectedEntities);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        CloudPotion potion = new CloudPotion(this.getPotionType());
        for (LivingEntity living : event.getAffectedEntities()) {
            potion.apply(living, event.getIntensity(living), 1.0f, damageSource, this, PotionEffectCause.POTION_SPLASH);
        }
    }
}
