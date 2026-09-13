package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.EnderPearl;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportCause;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.PortalParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.concurrent.ThreadLocalRandom;

public class EntityEnderPearl extends EntityProjectile implements EnderPearl {

    private boolean resolved;

    public EntityEnderPearl(EntityType<EnderPearl> type, Location location) {
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
        return 0.03f;
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

        if (this.isCollided) {
            resolveImpact();
        }

        if (this.age > 1200 || this.isCollided) {
            this.kill();
            hasUpdate = true;
        }

        this.timing.stopTiming();

        return hasUpdate;
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        super.onCollideWithEntity(entity);
        resolveImpact();
    }

    private void resolveImpact() {
        if (this.resolved) {
            return;
        }
        this.resolved = true;

        for (int i = 0; i < 32; i++) {
            this.level.addParticle(new PortalParticle(this.getPosition().add(
                    ThreadLocalRandom.current().nextGaussian(),
                    ThreadLocalRandom.current().nextDouble() * 2,
                    ThreadLocalRandom.current().nextGaussian())));
        }

        Entity owner = this.getOwner();
        if (owner == null || !owner.isAlive() || owner instanceof CloudPlayer player && player.isSleeping()) {
            this.close();
            return;
        }

        Vector3f ownerPosition = owner.getPosition();
        Location destination = Location.from(this.getPosition(), owner.getYaw(), owner.getPitch(), this.level);
        if (this.isOnPortalCooldown() && owner instanceof CloudEntity cloudOwner) {
            cloudOwner.setPortalCooldown();
        }

        if (owner.teleport(destination, PlayerTeleportCause.ENDER_PEARL)) {
            owner.resetFallDistance();
            if (owner instanceof CloudPlayer) {
                spawnEndermite(ownerPosition);
                DamageSource source = DamageSource.builder(DamageTypes.ENDER_PEARL)
                        .directEntity(this).causingEntity(owner).location(this.getLocation()).build();
                owner.attack(new EntityDamageEvent(owner, source, 5f));
            }
        }

        this.level.addSound(this.getPosition(), Sound.MOB_ENDERMEN_PORTAL);
        this.close();
    }

    private void spawnEndermite(Vector3f position) {
        if (ThreadLocalRandom.current().nextFloat() >= 0.05f
                || !this.level.getGameRules().get(GameRules.DO_MOB_SPAWNING)
                || this.level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        Entity endermite = CloudEntityRegistry.get().newEntity(EntityTypes.ENDERMITE, Location.from(position, this.level));
        endermite.spawnToAll();
    }
}
