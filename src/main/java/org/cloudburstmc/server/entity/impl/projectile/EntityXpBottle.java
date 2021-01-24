package org.cloudburstmc.server.entity.impl.projectile;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.projectile.XpBottle;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.world.particle.EnchantParticle;
import org.cloudburstmc.server.world.particle.Particle;
import org.cloudburstmc.server.world.particle.SpellParticle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author xtypr
 */
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
        return 0.1f;
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

        if (this.isCollided) {
            this.kill();
            this.dropXp();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.kill();
        this.dropXp();
    }

    public void dropXp() {
        Particle particle1 = new EnchantParticle(this.getPosition());
        this.getWorld().addParticle(particle1);
        Particle particle2 = new SpellParticle(this.getPosition(), 0x00385dc6);
        this.getWorld().addParticle(particle2);

        this.getWorld().dropExpOrb(this.getPosition(), ThreadLocalRandom.current().nextInt(3, 12));
    }
}
