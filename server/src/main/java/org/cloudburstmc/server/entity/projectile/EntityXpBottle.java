package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.projectile.XpBottle;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.particle.EnchantParticle;

import java.util.concurrent.ThreadLocalRandom;

public class EntityXpBottle extends EntityProjectile implements XpBottle {

    public EntityXpBottle(EntityType<XpBottle> type, Location location) {
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
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean hasUpdate = super.onUpdate(currentTick);

        if (this.age > 1200) {
            this.kill();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    @Override
    protected void onCollideWithEntity(Entity entity) {
        this.dropXp();
    }

    @Override
    protected void onBlockCollision(BlockHitResult hit) {
        this.dropXp();
    }

    private void dropXp() {
        this.getLevel().addParticle(new EnchantParticle(this.getPosition()));
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.GLASS);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        this.getLevel().dropExpOrb(this.getPosition(), 3 + random.nextInt(5) + random.nextInt(5));
        this.close();
    }
}
