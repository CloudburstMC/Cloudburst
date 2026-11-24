package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.WitherSkull;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.ExplosionBlockInteraction;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;

public class EntityWitherSkull extends EntityProjectile implements WitherSkull {

    private static final float OWNED_DAMAGE = 8.0F;
    private static final float MAGIC_DAMAGE = 5.0F;
    private static final float KILL_HEALING = 5.0F;
    private static final float EXPLOSION_RADIUS = 1.0F;

    public EntityWitherSkull(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void onCollideWithEntity(Entity target) {
        if (this.isClosed()) {
            return;
        }

        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity living ? living : null;
        DamageSource source = livingOwner != null ? this.createProjectileDamageSource() : DamageSource.builder(DamageTypes.MAGIC).directEntity(this).build();
        if (!CloudEntity.applyDamage(target, livingOwner != null ? OWNED_DAMAGE : MAGIC_DAMAGE, source)) {
            return;
        }

        if (!target.isAlive()) {
            if (livingOwner != null) {
                livingOwner.heal(KILL_HEALING, RegainReason.WITHER);
            }
        } else if (target instanceof CloudLivingEntity living) {
            int duration = switch (this.getLevel().getDifficulty()) {
                case NORMAL -> 200;
                case HARD -> 800;
                case PEACEFUL, EASY -> 0;
            };

            if (duration > 0) {
                living.addPotionEffect(new PotionEffect(EffectTypes.WITHER, duration, 1), owner == null ? this : owner, PotionEffectCause.ATTACK);
            }
        }
    }

    @Override
    protected void afterHit() {
        if (this.isClosed()) {
            return;
        }

        ExplosionBlockInteraction blocks = this.getLevel().getGameRules().get(GameRules.MOB_GRIEFING) ? ExplosionBlockInteraction.DESTROY : ExplosionBlockInteraction.KEEP;
        this.getLevel().explode(this.getPosition(), new ExplosionSettings(EXPLOSION_RADIUS, blocks, false, this, null));
        this.remove(EntityRemoveCause.HIT);
    }
}
