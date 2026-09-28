package org.cloudburstmc.api.entity;

import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.player.PlayerTeleportCause;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.MountType;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Entity extends Damageable, Emitter {

    EntityType<?> getType();

    /**
     * Returns the item representing this entity without changing it or an inventory.
     *
     * @param includeData whether supported entity-specific data should be included
     * @return the picked item, or {@link ItemStack#EMPTY} when this entity has no item representation
     * @see EntityComponents#GET_PICK_ITEM
     */
    ItemStack getPickItem(boolean includeData);

    /**
     * Captures an immutable copy of this entity's persistent state for later recreation.
     * Players cannot be copied this way. Identity and placement data are not retained.
     *
     * @return the snapshot, or an empty optional when copying is unsupported
     */
    Optional<EntitySnapshot> createSnapshot();

    Level getLevel();

    Chunk getChunk();

    Server getServer();

    long getUniqueId();

    /**
     * Returns the resolved posture used to determine this entity's dimensions.
     * A movement state such as sneaking does not guarantee a matching pose.
     *
     * @return current posture
     */
    Pose getPose();

    /**
     * Returns the collision height for the current pose in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision height before scaling
     */
    float getHeight();

    /**
     * Returns the vertical eye offset from the entity's position in blocks.
     *
     * @return eye height for the current pose
     */
    float getEyeHeight();

    double getHeadYaw();

    void setHeadYaw(double headYaw);

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

    /**
     * Returns the collision width along the X axis in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision width before scaling
     */
    float getWidth();

    /**
     * Returns the collision length along the Z axis in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision length before scaling
     */
    float getLength();

    boolean canCollide();

    /**
     * Returns how this entity reacts to block-driven displacement.
     *
     * @return push reaction
     */
    default PushReaction getPushReaction() {
        return PushReaction.NORMAL;
    }

    void onEntityCollision(Entity entity);

    float getGravity();

    float getDrag();

    boolean hasNameTag();

    String getNameTag();

    void setNameTag(String name);

    /**
     * Returns the entity's scoreboard tags.
     *
     * @return immutable scoreboard tags
     */
    Set<String> getScoreboardTags();

    /**
     * Returns whether the entity has a scoreboard tag.
     *
     * @param tag tag to test
     * @return {@code true} when the tag is present
     */
    boolean hasScoreboardTag(String tag);

    /**
     * Adds a scoreboard tag to the entity.
     *
     * <p>An entity can have at most 1024 scoreboard tags.</p>
     *
     * @param tag tag to add
     * @return {@code true} when the tag was added
     */
    boolean addScoreboardTag(String tag);

    /**
     * Removes a scoreboard tag from the entity.
     *
     * @param tag tag to remove
     * @return {@code true} when the tag was removed
     */
    boolean removeScoreboardTag(String tag);

    boolean isNameTagVisible();

    void setNameTagVisible(boolean visible);

    /**
     * @return size multiplier applied to the entity's collision dimensions
     */
    float getScale();

    /**
     * Changes the entity's scale and refreshes its collision bounds.
     *
     * @param scale size multiplier
     */
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

    /**
     * Returns the plain name used to identify this entity.
     *
     * @return the entity name
     */
    String getName();

    /**
     * Returns the name used when presenting this entity to an audience.
     *
     * @return the entity display name
     */
    Component displayName();

    /**
     * Adds this configured entity to its level.
     *
     * @return whether the entity was spawned
     */
    boolean spawn();

    void spawnTo(Player player);

    void spawnToAll();

    void despawnFrom(Player player);

    void despawnFromAll();

    /**
     * Returns an unmodifiable snapshot of players currently tracking this entity.
     *
     * @return the entity viewers
     */
    Set<? extends Player> getViewers();

    /**
     * Returns the number of ticks this entity has been freezing.
     *
     * @return the current freeze ticks
     */
    int getFreezeTicks();

    /**
     * Sets the number of ticks this entity has been freezing.
     *
     * @param ticks the new freeze ticks
     */
    void setFreezeTicks(int ticks);

    /**
     * Returns the number of freeze ticks required to fully freeze this entity.
     *
     * @return the maximum freeze ticks
     */
    int getMaxFreezeTicks();

    /**
     * Returns whether this entity is fully frozen.
     *
     * @return {@code true} if this entity is fully frozen
     */
    default boolean isFrozen() {
        return getFreezeTicks() >= getMaxFreezeTicks();
    }

    /**
     * Returns whether automatic freezing and thawing are disabled.
     *
     * @return {@code true} if freeze ticks are locked
     */
    boolean isFreezeTickingLocked();

    /**
     * Sets whether automatic freezing and thawing are disabled.
     *
     * @param locked whether freeze ticks are locked
     */
    void lockFreezeTicks(boolean locked);

    boolean canCollideWith(Entity entity);

    boolean canBeCollidedWith(@Nullable Entity entity);

    Direction getDirection();

    Vector3f getDirectionVector();

    Vector2f getDirectionPlane();

    Direction getHorizontalDirection();

    boolean onUpdate(int currentTick);

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

    /**
     * Returns the current world-space collision bounds, including pose and scale.
     * The returned box is immutable and does not track later changes.
     *
     * @return current collision bounds
     */
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

    /**
     * Teleports this entity to a position in its current level.
     *
     * @param position the destination
     * @return whether the teleport succeeded
     */
    default boolean teleport(Vector3f position) {
        return this.teleport(position, PlayerTeleportCause.PLUGIN);
    }

    /**
     * Teleports this entity to a position in its current level.
     *
     * @param position the destination
     * @param cause    the cause of the teleport
     * @return whether the teleport succeeded
     */
    boolean teleport(Vector3f position, PlayerTeleportCause cause);

    /**
     * Teleports this entity to a location.
     *
     * @param location the destination
     * @return whether the teleport succeeded
     */
    default boolean teleport(Location location) {
        return this.teleport(location, PlayerTeleportCause.PLUGIN);
    }

    /**
     * Teleports this entity to a location.
     *
     * @param location the destination
     * @param cause    the cause of the teleport
     * @return whether the teleport succeeded
     */
    boolean teleport(Location location, PlayerTeleportCause cause);

    @Nullable
    Entity getOwner();

    void setOwner(@Nullable Entity entity);

    void setMovementSpeed(float speed);

    float getMovementSpeed();

    //SyncedEntityData getData();

    boolean isClosed();

    void close();

}
