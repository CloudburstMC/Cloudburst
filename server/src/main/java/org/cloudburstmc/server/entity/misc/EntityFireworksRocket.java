package org.cloudburstmc.server.entity.misc;

import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.projectile.EntityProjectile;
import org.cloudburstmc.server.item.serializer.FireworkRocketSerializer;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityFireworksRocket extends EntityProjectile implements FireworksRocket {
    private static final double GLIDE_BOOST_TARGET_SPEED = 1.5;
    private static final double GLIDE_BOOST_DIRECT_PUSH = 0.1;
    private static final double GLIDE_BOOST_CORRECTION = 0.5;
    private static final FireworkData DEFAULT_FIREWORK_DATA = FireworkData.of(List.of(), (byte) 1);

    private int life;
    private int lifetime;
    @Setter
    private boolean shotAtAngle;

    private ItemStack firework;
    private CloudPlayer boostedPlayer;

    public EntityFireworksRocket(EntityType<FireworksRocket> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.updateLifetime();

        Random rand = ThreadLocalRandom.current();
        this.setMotion(Vector3f.from(rand.nextGaussian() * 0.001, 0.05, rand.nextGaussian() * 0.001));

        this.data.set(DISPLAY_FIREWORK, this.createFireworkDisplayData(DEFAULT_FIREWORK_DATA));
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Life", v -> this.life = v);
        tag.listenForInt("LifeTime", v -> this.lifetime = v);
        tag.listenForBoolean("ShotAtAngle", v -> this.shotAtAngle = v);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("Life", this.life);
        tag.putInt("LifeTime", this.lifetime);
        tag.putBoolean("ShotAtAngle", this.shotAtAngle);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        this.timing.startTiming();

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {
            if (this.boostedPlayer != null) {
                this.updateBoostedFlight();
            } else {
                this.updateFreeFlight();
            }

            if (!this.isAlive()) {
                this.timing.stopTiming();
                return true;
            }

            if (this.life == 0) {
                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.LAUNCH);
            }

            this.life++;

            hasUpdate = true;
            if (this.life > this.lifetime) {
                this.explode();
            }
        }

        this.timing.stopTiming();

        return hasUpdate || !this.onGround ||
                Math.abs(this.motion.getX()) > 0.00001 ||
                Math.abs(this.motion.getY()) > 0.00001 ||
                Math.abs(this.motion.getZ()) > 0.00001;
    }

    private void updateFreeFlight() {
        double horizontalAcceleration = this.isCollidedHorizontally ? 1.0 : 1.15;
        Vector3f nextMotion = this.shotAtAngle ? this.motion
                : this.motion.mul(horizontalAcceleration, 1.0, horizontalAcceleration).add(0, 0.04, 0);
        Vector3f nextPosition = this.position.add(nextMotion);
        RayTraceContext trace = new RayTraceContext(this.position, nextPosition, BlockShapeMode.COLLIDER,
                FluidCollisionMode.NONE, CollisionContext.of(this));

        float entityHitMargin = Math.clamp((this.age - 2) / 20.0f, 0.0f, 0.3f);
        HitResult hit = this.traceMovement(trace, entityHitMargin);

        if (hit instanceof MissHitResult(Vector3f boundary, MissReason reason) && reason == MissReason.UNLOADED) {
            this.setPosition(boundary);
            this.updateMovement();
            return;
        }

        this.move(nextMotion);
        this.motion = nextMotion;

        if (hit instanceof EntityHitResult entityHit) {
            ProjectileHitEvent event = new ProjectileHitEvent(this, entityHit);
            this.server.getEventManager().fire(event);
            if (!event.isCancelled()) {
                this.explode();
                return;
            }

            hit = this.level.rayTraceBlocks(trace);
        }

        if (hit instanceof BlockHitResult blockHit) {
            ProjectileHitEvent event = new ProjectileHitEvent(this, blockHit);
            this.server.getEventManager().fire(event);
            if (!event.isCancelled()) {
                blockHit.block().requireComponent(BlockComponents.ON_PROJECTILE_HIT).execute(blockHit.block(), this);
                if (!this.getFireworkData().getExplosions().isEmpty()) {
                    this.explode();
                    return;
                }
            } else {
                this.setPosition(nextPosition);
            }
        }

        this.updateRotationFromMotion();
        this.updateMovement();
    }

    private void explode() {
        this.updateMovement();
        EntityEventPacket packet = new EntityEventPacket();
        packet.setType(EntityEventType.FIREWORK_EXPLODE);
        packet.setRuntimeEntityId(this.getRuntimeId());
        CloudServer.broadcastPacket(this.getViewers(), packet);
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.LARGE_BLAST, -1, this.getType());
        this.dealExplosionDamage();
        this.kill();
    }

    private void dealExplosionDamage() {
        int explosions = this.getFireworkData().getExplosions().size();
        if (explosions == 0) {
            return;
        }

        Vector3f center = this.getPosition();
        DamageSource.Builder source = DamageSource.builder(DamageTypes.FIREWORKS).directEntity(this);
        Entity owner = this.getOwner();
        if (owner != null) {
            source.causingEntity(owner);
        }

        DamageSource damageSource = source.build();
        BoundingBox area = new BoundingBox(center, center).inflate(5, 5, 5);
        for (Entity entity : this.getLevel().getNearbyEntities(area)) {
            if (!(entity instanceof Living) || entity.getPosition().distanceSquared(center) > 25) {
                continue;
            }

            if (Explosion.getSeenPercent(center, entity) > 0) {
                float distance = (float) entity.getPosition().distance(center);
                entity.damage((5 + explosions * 2) * (float) Math.sqrt((5 - distance) / 5), damageSource);
            }
        }
    }

    private void updateBoostedFlight() {
        if (!this.boostedPlayer.isOnline() || !this.boostedPlayer.isAlive()) {
            this.clearBoostedPlayer();
            return;
        }

        if (this.boostedPlayer.isGliding()) {
            Vector3f look = this.boostedPlayer.getDirectionVector();
            Vector3f playerMotion = this.boostedPlayer.getMotion();
            Vector3f boost = Vector3f.from(
                    look.getX() * GLIDE_BOOST_DIRECT_PUSH + (look.getX() * GLIDE_BOOST_TARGET_SPEED - playerMotion.getX()) * GLIDE_BOOST_CORRECTION,
                    look.getY() * GLIDE_BOOST_DIRECT_PUSH + (look.getY() * GLIDE_BOOST_TARGET_SPEED - playerMotion.getY()) * GLIDE_BOOST_CORRECTION,
                    look.getZ() * GLIDE_BOOST_DIRECT_PUSH + (look.getZ() * GLIDE_BOOST_TARGET_SPEED - playerMotion.getZ()) * GLIDE_BOOST_CORRECTION
            );
            this.boostedPlayer.setPredictedMotion(playerMotion.add(boost));
        }

        this.position = this.boostedPlayer.getPosition();
        this.motion = this.boostedPlayer.getMotion();
        this.recalculateBoundingBox();
        this.updateRotationFromMotion();
        this.updateMovement();
    }

    private void updateRotationFromMotion() {
        float horizontalLength = (float) Math.sqrt(this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ());
        this.yaw = (float) (Math.atan2(this.motion.getX(), this.motion.getZ()) * (180D / Math.PI));
        this.pitch = (float) (Math.atan2(this.motion.getY(), horizontalLength) * (180D / Math.PI));
    }

    @Override
    protected boolean applyDamage(EntityDamageEvent source) {
        return (source.getDamageType() == DamageTypes.OUT_OF_WORLD ||
                source.getDamageType() == DamageTypes.ON_FIRE ||
                source.getDamageType().is(DamageTypeTags.IS_EXPLOSION))
                && super.applyDamage(source);
    }

    @Override
    public int getLife() {
        return life;
    }

    @Override
    public void setLife(int life) {
        this.life = life;
    }

    @Override
    public int getLifetime() {
        return lifetime;
    }

    @Override
    public void setLifetime(int lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public FireworkData getFireworkData() {
        FireworkData fireworkData = this.firework != null ? this.firework.get(ItemDataComponents.FIREWORK_DATA) : null;
        return fireworkData != null ? fireworkData : DEFAULT_FIREWORK_DATA;
    }

    @Override
    public void setFireworkData(@Nullable FireworkData data) {
        ItemStackBuilder builder = ItemStack.builder(ItemTypes.FIREWORK_ROCKET);
        FireworkData fireworkData = data != null ? data : DEFAULT_FIREWORK_DATA;
        builder.setData(ItemDataComponents.FIREWORK_DATA, fireworkData);

        this.firework = builder.build();
        this.data.set(EntityDataTypes.DISPLAY_FIREWORK, this.createFireworkDisplayData(fireworkData));
        this.updateLifetime();
    }

    private NbtMap createFireworkDisplayData(FireworkData fireworkData) {
        return NbtMap.builder()
                .putCompound("Fireworks", FireworkRocketSerializer.serializeFireworks(fireworkData))
                .build();
    }

    @Override
    public @Nullable Player getBoostedPlayer() {
        return this.boostedPlayer;
    }

    @Override
    public void setBoostedPlayer(@Nullable Player player) {
        if (player == null) {
            this.clearBoostedPlayer();
            return;
        }

        if (this.boostedPlayer == player) {
            return;
        }

        this.clearBoostedPlayer();

        this.boostedPlayer = (CloudPlayer) player;
        this.data.set(FIREWORK_SHOOTER_ID, this.boostedPlayer.getRuntimeId());
        this.data.set(FIREWORK_DIRECTION, this.boostedPlayer.getDirectionVector());
        this.boostedPlayer.beginFireworkGlideBoost(this.getRuntimeId());
    }

    private void clearBoostedPlayer() {
        if (this.boostedPlayer != null) {
            this.boostedPlayer.endFireworkGlideBoost(this.getRuntimeId());
            this.boostedPlayer = null;
        }
    }

    @Override
    public void kill() {
        this.clearBoostedPlayer();
        super.kill();
    }

    @Override
    public void close() {
        this.clearBoostedPlayer();
        super.close();
    }

    private void updateLifetime() {
        Random rand = ThreadLocalRandom.current();
        int flightLevel = 1 + this.getFireworkData().getFlightLevel();
        this.lifetime = 10 * flightLevel + rand.nextInt(6) + rand.nextInt(7);
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }
}
