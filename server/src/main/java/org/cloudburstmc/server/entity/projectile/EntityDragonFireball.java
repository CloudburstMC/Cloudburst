package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.projectile.DragonFireball;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.misc.EntityAreaEffectCloud;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public class EntityDragonFireball extends EntityProjectile implements DragonFireball {

    private static final float SPLASH_RANGE = 4.0F;
    private static final float INITIAL_RADIUS = 3.0F;
    private static final float FINAL_RADIUS = 7.0F;
    private static final int CLOUD_DURATION = 600;

    public EntityDragonFireball(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        // The breath cloud, rather than the projectile, damages entities.
    }

    @Override
    protected void afterHit() {
        if (this.isClosed()) {
            return;
        }

        Vector3f impact = this.getPosition();
        Vector3f center = impact;
        for (Entity nearby : this.getLevel().getNearbyEntities(
                this.getBoundingBox().inflate(SPLASH_RANGE, 2.0F, SPLASH_RANGE),
                entity -> entity instanceof LivingEntity && entity.isAlive()
                        && !(entity instanceof Player player && player.isSpectator()))) {
            Vector3f position = nearby.getPosition();
            if (position.sub(impact).lengthSquared() < SPLASH_RANGE * SPLASH_RANGE) {
                center = position;
                break;
            }
        }

        EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) CloudEntityRegistry.get()
                .newEntity(EntityTypes.AREA_EFFECT_CLOUD, Location.from(center, this.getLevel()));
        this.configureCloud(cloud);
        cloud.spawn();
        this.remove(EntityRemoveCause.HIT);
    }

    private void configureCloud(EntityAreaEffectCloud cloud) {
        if (this.getOwner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }

        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(INITIAL_RADIUS);
        cloud.setDuration(CLOUD_DURATION);
        cloud.setWaitTime(20);
        cloud.setReapplicationDelay(20);
        cloud.setRadiusOnUse(0.0F);
        cloud.setRadiusPerTick((FINAL_RADIUS - INITIAL_RADIUS) / CLOUD_DURATION);
        cloud.setPotionDurationScale(0.25F);
        cloud.addCustomEffect(new PotionEffect(EffectTypes.INSTANT_DAMAGE, 1, 1), true);
    }
}
