package org.cloudburstmc.api.entity;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.misc.LightningBolt;
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
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    default float getBaseOffset() {
        return 0f;
    }

    default Vector3f getPassengerAttachmentPoint(Entity passenger) {
        return Vector3f.from(0f, getHeight(), 0f);
    }

    default float getMountedHeightOffset() {
        return getHeight() * 0.75f;
    }

    default float getPassengerHeightOffset() {
        return 0f;
    }

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

    void setSeatPosition(Vector3f position);

    Entity getVehicle();

    default boolean mount(Entity entity) {
        return this.mount(entity, MountType.RIDER);
    }

    /**
     * Mounts this entity onto another entity.
     *
     * @param vehicle the vehicle to mount
     * @param mode    the mount mode
     * @return {@code true} if the entity was mounted
     */
    boolean mount(Entity vehicle, MountType mode);

    boolean dismount(Entity vehicle);

    void onMount(Entity passenger);

    void onDismount(Entity passenger);

    Map<EffectType, Effect> getEffects();

    void removeAllEffects();

    void addEffect(Effect effect);

    /**
     * Gets an effect by its numeric id.
     *
     * @param effectId the effect id
     * @return the effect, or {@code null} if this entity does not have it
     * @deprecated use {@link #getEffect(EffectType)}
     */
    @Deprecated
    Effect getEffect(int effectId);

    Effect getEffect(EffectType type);

    /**
     * Removes an effect by its numeric id.
     *
     * @param effectId the effect id
     * @deprecated use {@link #removeEffect(EffectType)}
     */
    @Deprecated
    void removeEffect(int effectId);

    void removeEffect(EffectType type);

    /**
     * Tests whether this entity has an effect by its numeric id.
     *
     * @param effectId the effect id
     * @return {@code true} if this entity has the effect
     * @deprecated use {@link #hasEffect(EffectType)}
     */
    @Deprecated
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

    boolean canBeCollidedWith(@Nullable Entity entity);

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

    BoundingBox getBoundingBox();

    /**
     * Tests whether this entity would collide at a location.
     *
     * @param location the target location
     * @return {@code true} if this entity's bounding box would collide at the location
     */
    default boolean collidesAt(Location location) {
        Vector3f movement = location.getPosition().sub(this.getPosition());
        return location.getLevel().hasCollision(this, this.getBoundingBox().move(movement));
    }

    /**
     * Tests whether this entity would collide using a supplied bounding box.
     *
     * @param boundingBox the box to test
     * @return {@code true} if the box collides in this entity's level
     */
    default boolean wouldCollideUsing(BoundingBox boundingBox) {
        return this.getLevel().hasCollision(this, boundingBox);
    }

    void fall(float fallDistance);

    void onStruckByLightning(LightningBolt lightningBolt);

    boolean onInteract(Player player, ItemStack item, Vector3f clickedPos);

    float getX();

    float getY();

    float getZ();

    Vector3f getPosition();

    boolean setPosition(Vector3f position);

    Location getLocation();

    Vector3f getMotion();

    boolean setMotion(Vector3f motion);

    void makeStuckInBlock(BlockState state, Vector3f speedMultiplier);

    void setRotation(float yaw, float pitch);

    boolean setPositionAndRotation(Vector3f position, float yaw, float pitch);

    float getPitch();

    float getYaw();

    boolean canBeMovedByCurrents();

    /**
     * Tests whether this entity can activate pressure plates.
     *
     * @return {@code true} if this entity can activate pressure plates
     */
    boolean canTriggerPressurePlate();

    boolean isPushable();

    boolean isOnGround();

    void setOnGround(boolean onGround);

    Optional<Vector3i> getSupportingBlockPosition();

    default boolean isSupportedBy(Vector3i position) {
        return this.getSupportingBlockPosition().filter(position::equals).isPresent();
    }

    default boolean isUndead() {
        return false;
    }

    void kill();

    default boolean teleport(Vector3f position) {
        return this.teleport(position, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    boolean teleport(Vector3f position, PlayerTeleportEvent.TeleportCause cause);

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
