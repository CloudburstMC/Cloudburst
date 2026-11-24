package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.ThrownExperienceBottle;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.entity.misc.EntityExperienceOrb;

import java.util.concurrent.ThreadLocalRandom;

public class EntityThrownExperienceBottle extends EntityProjectile implements ThrownExperienceBottle {

    public EntityThrownExperienceBottle(EntityType<ThrownExperienceBottle> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.07f;
    }

    @Override
    public float getDrag() {
        return 0.01f;
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = super.tickEntity(currentTick, elapsedTicks);
        if (this.age > 1200) {
            this.remove(EntityRemoveCause.DESPAWN);
            hasUpdate = true;
        }

        return hasUpdate;
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        this.dropXp(this.getMotion().mul(-1));
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        Direction face = hit.face();
        this.dropXp(face == null ? this.getMotion().mul(-1) : face.getUnitVector().toFloat());
    }

    private void dropXp(Vector3f outward) {
        if (this.isClosed()) {
            return;
        }

        this.remove(EntityRemoveCause.HIT);
        this.getLevel().spawnParticle(ParticleTypes.MOB_SPELL, this.getPosition());
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.GLASS);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int experience = 3 + random.nextInt(5) + random.nextInt(5);
        EntityExperienceOrb.forEachOrbValue(experience, value -> {
            Vector3f motion = Vector3f.from(
                    (random.nextDouble() * 0.2 - 0.1) * 2,
                    random.nextDouble() * 0.4,
                    (random.nextDouble() * 0.2 - 0.1) * 2
            );

            if (outward.dot(motion) < 0) {
                motion = motion.mul(-1);
            }

            this.getLevel().spawnExperienceOrb(this.getPosition(), value, motion, 0);
        });
    }
}
