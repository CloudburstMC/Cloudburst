package org.cloudburstmc.server.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.projectile.FireworkRocket;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
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
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudFireworkRocketData;
import org.cloudburstmc.server.item.serializer.FireworkRocketSerializer;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityFireworkRocket extends EntityProjectile implements FireworkRocket {

    private static final double GLIDE_BOOST_TARGET_SPEED = 1.5;
    private static final double GLIDE_BOOST_DIRECT_PUSH = 0.1;
    private static final double GLIDE_BOOST_CORRECTION = 0.5;
    private static final FireworkData DEFAULT_FIREWORK_DATA = FireworkData.of(List.of(), (byte) 1);

    private CloudPlayer boostedPlayer;

    private final CloudFireworkRocketData persistenceState;

    public EntityFireworkRocket(EntityType<FireworkRocket> type, Location location) {
        CloudFireworkRocketData state = new CloudFireworkRocketData();
        super(type, location, state);
        this.persistenceState = state;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.updateLifetime();

        Random rand = ThreadLocalRandom.current();
        this.setMotion(Vector3f.from(rand.nextGaussian() * 0.001, 0.05, rand.nextGaussian() * 0.001));

        this.updateFireworkDisplay();
    }

    @Override
    protected CloudFireworkRocketData data() {
        return this.persistenceState;
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        this.updateFireworkDisplay();
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (this.isClosed()) {
            return false;
        }

        boolean hasUpdate = this.entityBaseTick(elapsedTicks);

        if (this.isAlive()) {
            if (this.boostedPlayer != null) {
                this.updateBoostedFlight();
            } else {
                this.updateFreeFlight();
            }

            if (!this.isAlive()) {
                return true;
            }

            if (this.getTicksFlown() == 0) {
                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.LAUNCH);
            }

            this.setTicksFlown(this.getTicksFlown() + 1);

            hasUpdate = true;
            if (this.getTicksFlown() > this.getTicksToDetonate()) {
                this.explode();
            }
        }

        return hasUpdate || !this.isOnGround() ||
                Math.abs(this.motion.getX()) > 0.00001 ||
                Math.abs(this.motion.getY()) > 0.00001 ||
                Math.abs(this.motion.getZ()) > 0.00001;
    }

    private void updateFreeFlight() {
        double horizontalAcceleration = this.hasHorizontalCollision() ? 1.0 : 1.15;
        Vector3f nextMotion = this.isShotAtAngle() ? this.motion
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
            this.dispatchHitEvent(event);
            if (this.isClosed()) {
                return;
            }

            if (!event.isCancelled()) {
                this.explode();
                return;
            }

            hit = this.level.rayTraceBlocks(trace);
        }

        if (hit instanceof BlockHitResult blockHit) {
            ProjectileHitEvent event = new ProjectileHitEvent(this, blockHit);
            this.dispatchHitEvent(event);
            if (this.isClosed()) {
                return;
            }

            if (!event.isCancelled()) {
                this.notifyHitBlock(blockHit);
            }

            if (this.isClosed()) {
                return;
            }

            if (!this.getFireworkData().getExplosions().isEmpty()) {
                this.explode();
                return;
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
        this.remove(EntityRemoveCause.EXPLODE);
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
            if (!(entity instanceof LivingEntity) || entity.getPosition().distanceSquared(center) > 25) {
                continue;
            }

            if (Explosion.getSeenPercent(center, entity) > 0) {
                float distance = entity.getPosition().distance(center);
                CloudEntity.applyDamage(entity, (5 + explosions * 2) * (float) Math.sqrt((5 - distance) / 5), damageSource);
            }
        }
    }

    private void updateBoostedFlight() {
        if (!this.boostedPlayer.isOnline() || !this.boostedPlayer.isAlive() || this.boostedPlayer.getLevel() != this.getLevel()) {
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
    public int getTicksFlown() {
        return this.data().getTicksFlown();
    }

    @Override
    public void setTicksFlown(int ticks) {
        this.data().setTicksFlown(ticks);
    }

    @Override
    public int getTicksToDetonate() {
        return this.data().getTicksToDetonate();
    }

    @Override
    public void setTicksToDetonate(int ticks) {
        this.data().setTicksToDetonate(ticks);
    }

    @Override
    public boolean isShotAtAngle() {
        return this.data().isShotAtAngle();
    }

    @Override
    public void setShotAtAngle(boolean shotAtAngle) {
        this.data().setShotAtAngle(shotAtAngle);
    }

    @Override
    public FireworkData getFireworkData() {
        ItemStack firework = this.data().getFirework();
        FireworkData fireworkData = firework == null ? null : firework.get(ItemDataComponents.FIREWORK_DATA);
        return fireworkData != null ? fireworkData : DEFAULT_FIREWORK_DATA;
    }

    @Override
    public void setFireworkData(@Nullable FireworkData fireworkData) {
        this.data().setFirework(fireworkData == null ? null : ItemStack.builder(ItemTypes.FIREWORK_ROCKET)
                .setData(ItemDataComponents.FIREWORK_DATA, fireworkData)
                .build());
        this.updateFireworkDisplay();
        this.updateLifetime();
    }

    private void updateFireworkDisplay() {
        this.data.set(DISPLAY_FIREWORK, this.createFireworkDisplayData(this.getFireworkData()));
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
    public void remove(EntityRemoveCause cause) {
        this.clearBoostedPlayer();
        super.remove(cause);
    }

    private void updateLifetime() {
        Random rand = ThreadLocalRandom.current();
        int flightLevel = 1 + this.getFireworkData().getFlightLevel();
        this.setTicksToDetonate(10 * flightLevel + rand.nextInt(6) + rand.nextInt(7));
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
