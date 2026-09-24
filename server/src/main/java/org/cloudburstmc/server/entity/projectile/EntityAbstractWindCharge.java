package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.AbstractWindCharge;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEventType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.level.particle.GenericParticle;
import org.cloudburstmc.server.player.CloudPlayer;

public abstract class EntityAbstractWindCharge extends EntityProjectile implements AbstractWindCharge {

    private static final float SIZE = 0.3125f;

    protected EntityAbstractWindCharge(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return SIZE;
    }

    @Override
    public float getLength() {
        return SIZE;
    }

    @Override
    public float getHeight() {
        return SIZE;
    }

    @Override
    public float getGravity() {
        return 0;
    }

    @Override
    public float getDrag() {
        return 0;
    }

    @Override
    protected float getWaterInertia() {
        return 1;
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        entity.damage(1, this.createProjectileDamageSource());
        this.burst(this.getPosition());
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        Vector3f position = hit.position();
        if (hit.face() != null) {
            position = position.add(hit.face().getUnitVector().toFloat().mul(0.25f));
        }
        this.burst(position);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        boolean updated = super.onUpdate(currentTick);
        if (this.age > 1200) {
            this.close();
        }

        return updated;
    }

    protected abstract float burstRadius();

    protected float burstKnockbackMultiplier() {
        return 1;
    }

    protected abstract LevelEventType burstParticle();

    protected abstract SoundEvent burstSound();

    @Override
    public void explode() {
        if (!this.closed) {
            this.burst(this.getPosition());
        }
    }

    private void burst(Vector3f position) {
        float reach = this.burstRadius() * 2;
        for (Entity entity : this.getLevel().getNearbyEntities(new BoundingBox(position, position).inflate(reach, reach, reach))) {
            if (entity == this) {
                continue;
            }

            Vector3f origin = entity.getPosition();
            Vector3f delta = origin.add(0, entity.getEyeHeight(), 0).sub(position);
            double distance = origin.distance(position);
            if (delta.lengthSquared() < 0.000001 || distance >= reach) {
                continue;
            }

            if (entity instanceof CloudPlayer flyingPlayer && flyingPlayer.getAbilities().get(Ability.FLYING)) {
                continue;
            }

            float strength = (float) ((1 - distance / reach) * Explosion.getSeenPercent(position, entity) * this.burstKnockbackMultiplier());
            if (strength <= 0) {
                continue;
            }

            Vector3f movement = entity instanceof CloudPlayer player ? player.getKnownMovement() : entity.getMotion();
            entity.setMotion(movement.add(delta.normalize().mul(strength)));
        }

        this.getLevel().addParticle(new GenericParticle(position, this.burstParticle()));
        this.getLevel().addLevelSoundEvent(position, this.burstSound());
        this.close();
    }
}
