package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.serializer.FireworkRocketSerializer;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public class EntityFireworksRocket extends CloudEntity implements FireworksRocket {
    private static final double GLIDE_BOOST_TARGET_SPEED = 1.5;
    private static final double GLIDE_BOOST_DIRECT_PUSH = 0.1;
    private static final double GLIDE_BOOST_CORRECTION = 0.5;
    private static final FireworkData DEFAULT_FIREWORK_DATA = FireworkData.of(List.of(), (byte) 1);

    private int life;
    private int lifetime;

    private ItemStack firework;
    private CloudPlayer boostedPlayer;

    public EntityFireworksRocket(EntityType<FireworksRocket> type, Location location) {
        super(type, location);
    }

    @Override
    public float getBaseOffset() {
        return 0.49f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.updateLifetime();

        Random rand = ThreadLocalRandom.current();
        this.setMotion(Vector3f.from(rand.nextGaussian() * 0.001, 0.05, rand.nextGaussian() * 0.001));

        this.data.set(DISPLAY_FIREWORK, this.createFireworkDisplayData(DEFAULT_FIREWORK_DATA));
        this.data.set(DISPLAY_OFFSET, 0);
        this.data.set(CUSTOM_DISPLAY, (byte) 1);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Life", v -> this.life = v);
        tag.listenForInt("LifeTime", v -> this.lifetime = v);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("Life", this.life);
        tag.putInt("LifeTime", this.lifetime);
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

            if (this.life == 0) {
                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.LAUNCH);
            }

            this.life++;

            hasUpdate = true;
            if (this.life >= this.lifetime) {
                EntityEventPacket packet = new EntityEventPacket();
                packet.setType(EntityEventType.FIREWORK_EXPLODE);
                packet.setRuntimeEntityId(this.getRuntimeId());

                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.LARGE_BLAST, -1, getType());

                CloudServer.broadcastPacket(getViewers(), packet);

                this.kill();
                hasUpdate = true;
            }
        }

        this.timing.stopTiming();

        return hasUpdate || !this.onGround ||
                Math.abs(this.motion.getX()) > 0.00001 ||
                Math.abs(this.motion.getY()) > 0.00001 ||
                Math.abs(this.motion.getZ()) > 0.00001;
    }

    private void updateFreeFlight() {
        this.motion = this.motion.mul(1.15, 1.0, 1.15).add(0, 0.04, 0);
        this.setPosition(this.position.add(this.motion));
        this.updateRotationFromMotion();
        this.updateMovement();
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
    public boolean attack(EntityDamageEvent source) {
        return (source.getDamageType() == DamageTypes.VOID ||
                source.getDamageType() == DamageTypes.FIRE_TICK ||
                source.getDamageType().is(DamageTypeTags.IS_EXPLOSION))
                && super.attack(source);
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
        FireworkData fireworkData = this.firework != null ? this.firework.get(ItemKeys.FIREWORK_DATA) : null;
        return fireworkData != null ? fireworkData : DEFAULT_FIREWORK_DATA;
    }

    @Override
    public void setFireworkData(@Nullable FireworkData data) {
        ItemStackBuilder builder = ItemStack.builder(ItemTypes.FIREWORK_ROCKET);
        FireworkData fireworkData = data != null ? data : DEFAULT_FIREWORK_DATA;
        builder.data(ItemKeys.FIREWORK_DATA, fireworkData);

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
