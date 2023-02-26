package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

/**
 * @author CreeperFace
 */
public class EntityFireworksRocket extends BaseEntity implements FireworksRocket {

    private int life;
    private int lifetime;

    private ItemStack firework;

    public EntityFireworksRocket(EntityType<FireworksRocket> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        Random rand = ThreadLocalRandom.current();
        this.lifetime = 30 + rand.nextInt(6) + rand.nextInt(7);

        this.setMotion(Vector3f.from(rand.nextGaussian() * 0.001, 0.05, rand.nextGaussian() * 0.001));

        this.data.set(DISPLAY_FIREWORK, NbtMap.EMPTY);
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

            this.motion = motion.mul(1.15, 1.15, 0).add(0, 0, 0.04);
            this.move(this.motion);

            this.updateMovement();


            float f = (float) Math.sqrt(this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ());
            this.yaw = (float) (Math.atan2(this.motion.getX(), this.motion.getZ()) * (180D / Math.PI));

            this.pitch = (float) (Math.atan2(this.motion.getY(), f) * (180D / Math.PI));


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

    @Override
    public boolean attack(EntityDamageEvent source) {
        return (source.getCause() == EntityDamageEvent.DamageCause.VOID ||
                source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                source.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                source.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
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
        return this.firework != null ? this.firework.get(ItemKeys.FIREWORK_DATA) : null;
    }

    @Override
    public void setFireworkData(FireworkData data) {
        this.firework = ItemStack.builder(ItemTypes.FIREWORKS)
                .data(ItemKeys.FIREWORK_DATA, data)
                .build();
        this.data.set(EntityDataTypes.DISPLAY_FIREWORK, ItemUtils.serializeItem(this.firework));
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
