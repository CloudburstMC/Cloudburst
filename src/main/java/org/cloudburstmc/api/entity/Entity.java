package org.cloudburstmc.api.entity;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.entity.passive.Bat;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Entity {

    EntityType<?> getType();

    Level getLevel();

    Chunk getChunk();

    Server getServer();

    long getUniqueId();

    long getRuntimeId();

    float getHeight();

    float getEyeHeight();

    float getWidth();

    float getLength();

    boolean canCollide();

    void onEntityCollision(Entity entity);

    float getGravity();

    float getDrag();

    boolean hasNameTag();

    String getNameTag();

    void setNameTag(String name);

    boolean isNameTagVisible();

    void setNameTagVisible(boolean visible);

    float getScale();

    void setScale(float scale);

    List<? extends Entity> getPassengers();

    boolean isPassenger(Entity entity);

    boolean isControlling(Entity entity);

    boolean hasControllingPassenger();

    Vector3f getSeatPosition();

    void setSeatPosition(Vector3f pos);

    Entity getVehicle();

    default boolean mount(Entity entity) {
        return this.mount(entity, MountType.RIDER);
    }

    /**
     * Enter into a vehicle
     *
     * @param vehicle vehicle to mount
     * @param mode    mode
     * @return whether or not the mount was successful
     */
    boolean mount(Entity vehicle, MountType mode);

    boolean dismount(Entity vehicle);

    void onMount(Entity passenger);

    void onDismount(Entity passenger);

    Map<EffectType, Effect> getEffects();

    void removeAllEffects();

    void addEffect(Effect effect);

    @Deprecated
    /**
     * Use {@link #getEffect(EffectType)}
     */
    Effect getEffect(int effectId);

    Effect getEffect(EffectType type);

    @Deprecated
    /**
     * Use {@link #removeEffect(EffectType)}
     */
    void removeEffect(int effectId);

    void removeEffect(EffectType type);

    @Deprecated
    /**
     * Use {@link #hasEffect(EffectType)}
     */
    boolean hasEffect(int effectId);

    boolean hasEffect(EffectType type);

    String getName();

    void spawnTo(Player player);

    void spawnToAll();

    void despawnFrom(Player player);

    void despawnFromAll();

    Set<? extends Player> getViewers();

    default boolean attack(float damage) {
        return this.attack(new EntityDamageEvent(this, EntityDamageEvent.DamageCause.CUSTOM, damage));
    }

    boolean attack(EntityDamageEvent source);

    default void heal(float amount) {
        this.heal(new EntityRegainHealthEvent(this, amount, EntityRegainHealthEvent.CAUSE_REGEN));
    }

    void heal(EntityRegainHealthEvent source);

    float getHealth();

    void setHealth(float health);

    int getMaxHealth();

    void setMaxHealth(int maxHealth);

    default boolean isAlive() {
        return getHealth() > 0;
    }

    EntityDamageEvent getLastDamageCause();

    boolean canCollideWith(Entity entity);

    Direction getDirection();

    Vector3f getDirectionVector();

    Vector2f getDirectionPlane();

    Direction getHorizontalDirection();

    boolean onUpdate(int currentTick);

    float getAbsorption();

    void setAbsorption(float absorption);

    default boolean isOnFire() {
        return getFireTicks() > 0;
    }

    void setOnFire(@NonNegative int seconds);

    @NonNegative
    int getFireTicks();

    void extinguish();

    int getNoDamageTicks();

    void setNoDamageTicks(int noDamageTicks);

    float getHighestPosition();

    void setHighestPosition(float highestPosition);

    void resetFallDistance();

    AxisAlignedBB getBoundingBox();

    void fall(float fallDistance);

    void onStruckByLightning(LightningBolt lightningBolt);

    boolean onInteract(Player player, ItemStack item, Vector3f clickedPos);

    float getX();

    float getY();

    float getZ();

    Vector3f getPosition();

    boolean setPosition(Vector3f pos);

    Location getLocation();

    Vector3f getMotion();

    boolean setMotion(Vector3f motion);

    void setRotation(float yaw, float pitch);

    boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch);

    float getPitch();

    float getYaw();

    boolean canBeMovedByCurrents();

    /**
     * Whether the entity can active pressure plates.
     * Used for {@link Bat}s only.
     *
     * @return triggers pressure plate
     */
    boolean canTriggerPressurePlate();

    boolean canPassThrough();

    boolean isOnGround();

    void setOnGround(boolean onGround);

    default boolean isUndead() {
        return false;
    }

    void kill();

    default boolean teleport(Vector3f pos) {
        return this.teleport(pos, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    boolean teleport(Vector3f pos, PlayerTeleportEvent.TeleportCause cause);

    default boolean teleport(Location location) {
        return this.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause);

    @Nullable
    Entity getOwner();

    void setOwner(@Nullable Entity entity);

    //SyncedEntityData getData();

    boolean isClosed();

    void close();

}
