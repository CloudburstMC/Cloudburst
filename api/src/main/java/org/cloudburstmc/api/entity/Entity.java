package org.cloudburstmc.api.entity;

import net.kyori.adventure.sound.Sound.Emitter;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.TeleportCause;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.persistence.PersistentDataHolder;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * An entity in a level.
 */
public interface Entity extends Emitter, PersistentDataHolder {

    /**
     * @return the registered entity type
     */
    EntityType<?> getType();

    /**
     * Returns the identifier of this live entity instance. Do not persist it across sessions.
     */
    long getUniqueId();

    /**
     * @return the server that owns this entity
     */
    Server getServer();

    /**
     * @return the level containing this entity
     */
    Level getLevel();

    /**
     * @return the chunk containing this entity
     */
    Chunk getChunk();

    /**
     * Attempts to add this entity to its level. Non-player spawns may be canceled.
     *
     * @return whether the entity was spawned
     */
    boolean spawn();

    /**
     * Returns an unmodifiable snapshot of players currently tracking this entity.
     */
    Set<? extends Player> getViewers();

    /**
     * Returns whether this entity is alive and registered in a world.
     */
    boolean isValid();

    /**
     * Returns whether this entity is alive and has not been removed.
     */
    boolean isAlive();

    /**
     * Returns the last damage event applied to this entity, or {@code null} if none.
     */
    @Nullable
    EntityDamageEvent getLastDamageCause();

    /**
     * @return whether this entity has been removed and can no longer be updated
     */
    boolean isClosed();

    /**
     * Requests death rather than immediate removal. A living entity's death event
     * can cancel death and restore its configured revival health.
     */
    void kill();

    /**
     * Removes this entity from its level and viewers without triggering death or death drops.
     * For non-player entities, this is reported as a plugin removal.
     * Calling this again has no effect.
     */
    void close();

    /**
     * Captures an immutable copy of this entity's persistent state for later recreation.
     * Player-type entities, including standalone humans, cannot be copied this way.
     * Identity and placement data are not retained.
     *
     * @return the snapshot, or an empty optional when copying is unsupported
     */
    Optional<EntitySnapshot> createSnapshot();

    /**
     * Returns the item representing this entity without changing it or an inventory.
     *
     * @param includeData whether supported entity-specific data should be included
     * @return the picked item, or {@link ItemStack#EMPTY} when this entity has no item representation
     * @see EntityComponents#GET_PICK_ITEM
     */
    ItemStack getPickItem(boolean includeData);

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
     * Returns the entity's custom name, or {@code null} when no name is set.
     *
     * @return the custom name
     */
    @Nullable
    Component customName();

    /**
     * Sets the entity's custom name without changing its visibility.
     *
     * @param name the custom name, or {@code null} to clear it
     */
    void customName(@Nullable Component name);

    /**
     * @return whether the custom name may be displayed
     */
    boolean isCustomNameVisible();

    /**
     * @param visible whether to allow the custom name to be displayed
     */
    void setCustomNameVisible(boolean visible);

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

    /**
     * @return the current level, position and rotation
     */
    Location getLocation();

    /**
     * @return this entity's world-space position
     */
    Vector3f getPosition();

    /**
     * @return the world-space X coordinate
     */
    float getX();

    /**
     * @return the world-space Y coordinate
     */
    float getY();

    /**
     * @return the world-space Z coordinate
     */
    float getZ();

    /**
     * Changes position within the current level without resolving movement collisions.
     *
     * @param position the destination
     * @return whether the position was applied
     */
    boolean setPosition(Vector3f position);

    /**
     * @return horizontal rotation in degrees
     */
    float getYaw();

    /**
     * @return vertical rotation in degrees
     */
    float getPitch();

    /**
     * @param yaw   horizontal rotation in degrees
     * @param pitch vertical rotation in degrees
     */
    void setRotation(float yaw, float pitch);

    /**
     * Changes position and rotation within the current level without resolving movement collisions.
     *
     * @param position the destination
     * @param yaw      horizontal rotation in degrees
     * @param pitch    vertical rotation in degrees
     * @return whether the transform was applied, or {@code false} if the entity is closed
     * @throws IllegalArgumentException if any coordinate or angle is not finite
     */
    boolean setPositionAndRotation(Vector3f position, float yaw, float pitch);

    /**
     * @return the horizontal direction determined by this entity's yaw
     */
    Direction getDirection();

    /**
     * @return the unit direction vector determined by yaw and pitch
     */
    Vector3f getDirectionVector();

    /**
     * @return the horizontal direction determined by this entity's yaw
     */
    Direction getHorizontalDirection();

    /**
     * @return the unit horizontal direction vector in the X-Z plane
     */
    Vector2f getHorizontalLookVector();

    /**
     * Teleports this entity to a position in its current level.
     *
     * @param position the destination
     * @return whether the teleport succeeded
     */
    default boolean teleport(Vector3f position) {
        return this.teleport(position, TeleportCause.PLUGIN);
    }

    /**
     * Teleports this entity to a position in its current level.
     *
     * @param position the destination
     * @param cause    the cause of the teleport
     * @return whether the teleport succeeded
     */
    boolean teleport(Vector3f position, TeleportCause cause);

    /**
     * Teleports this entity to a location.
     *
     * @param location the destination
     * @return whether the teleport succeeded
     */
    default boolean teleport(Location location) {
        return this.teleport(location, TeleportCause.PLUGIN);
    }

    /**
     * Teleports this entity to a location.
     *
     * @param location the destination
     * @param cause    the cause of the teleport
     * @return whether the teleport succeeded
     */
    boolean teleport(Location location, TeleportCause cause);

    /**
     * Returns the remaining ticks before this entity can use another portal.
     */
    int getPortalCooldown();

    /**
     * Sets the remaining portal cooldown. Zero clears it.
     *
     * @throws IllegalArgumentException if the cooldown is negative
     */
    void setPortalCooldown(int cooldownTicks);

    /**
     * Returns the resolved posture used to determine this entity's dimensions.
     * A movement state such as sneaking does not guarantee a matching pose.
     *
     * @return current posture
     */
    Pose getPose();

    /**
     * @return size multiplier applied to the entity's collision dimensions
     */
    float getScale();

    /**
     * Changes the entity's scale and refreshes its collision bounds.
     *
     * @param scale size multiplier from 0.0625 through 16, inclusive
     * @throws IllegalArgumentException if the scale is outside that range or not finite
     */
    void setScale(float scale);

    /**
     * Returns the collision width along the X axis in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision width before scaling
     */
    float getWidth();

    /**
     * Returns the collision height for the current pose in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision height before scaling
     */
    float getHeight();

    /**
     * Returns the collision length along the Z axis in blocks.
     * Use {@link #getBoundingBox()} for the scaled world-space bounds.
     *
     * @return current pose's collision length before scaling
     */
    float getLength();

    /**
     * Returns the vertical eye offset from the entity's position in blocks.
     *
     * @return eye height for the current pose
     */
    float getEyeHeight();

    /**
     * Returns the current world-space collision bounds, including pose and scale.
     * The returned box is immutable and does not track later changes.
     *
     * @return current collision bounds
     */
    BoundingBox getBoundingBox();

    /**
     * @return whether this entity participates in entity collisions
     */
    boolean canCollide();

    /**
     * @param entity the other entity
     * @return whether this entity can collide with it
     */
    boolean canCollideWith(Entity entity);

    /**
     * @param entity the entity testing collision, or null for a general query
     * @return whether this entity presents a collision obstacle
     */
    boolean canBeCollidedWith(@Nullable Entity entity);

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

    /**
     * @return the current motion in blocks per tick
     */
    Vector3f getMotion();

    /**
     * Replaces motion unless a motion event cancels the change.
     *
     * @param motion the finite motion in blocks per tick
     * @return whether the motion was applied
     * @throws IllegalArgumentException if any component is not finite
     */
    boolean setMotion(Vector3f motion);

    /**
     * @return the downward acceleration used by this entity's movement
     */
    float getGravity();

    /**
     * @return the drag coefficient used by this entity's movement
     */
    float getDrag();

    /**
     * Returns how this entity reacts to block-driven displacement.
     *
     * @return push reaction
     */
    default PushReaction getPushReaction() {
        return PushReaction.NORMAL;
    }

    /**
     * @return whether other entities can push this entity
     */
    boolean isPushable();

    /**
     * @return whether liquid currents can change this entity's motion
     */
    boolean canBeMovedByCurrents();

    /**
     * @return whether this entity is resting on a collision surface
     */
    boolean isOnGround();

    /**
     * @return the supporting block position, or empty when no support is known
     */
    Optional<Vector3i> getSupportingBlockPosition();

    /**
     * @param position the block position to test
     * @return whether that block is this entity's current support
     */
    default boolean isSupportedBy(Vector3i position) {
        return this.getSupportingBlockPosition().filter(position::equals).isPresent();
    }

    /**
     * Returns the distance fallen since the last reset, in blocks.
     */
    float getFallDistance();

    /**
     * Sets the accumulated fall distance.
     *
     * @param distance the finite, non-negative distance in blocks
     * @throws IllegalArgumentException if the distance is negative or not finite
     */
    void setFallDistance(float distance);

    /**
     * Clears accumulated fall distance at the entity's current height.
     */
    void resetFallDistance();

    /**
     * @return the owning entity, or {@code null} when no owner is assigned
     */
    @Nullable
    Entity getOwner();

    /**
     * @param entity the owning entity, or {@code null} to clear ownership
     */
    void setOwner(@Nullable Entity entity);

    /**
     * @return the vehicle this entity rides, or {@code null} when not mounted
     */
    @Nullable
    Entity getVehicle();

    /**
     * Mounts this entity onto another entity in the same level.
     * Vehicle entry and exit events may prevent changing vehicles.
     * This explicit request bypasses sneaking and the normal delay after dismounting,
     * but still respects passenger capacity and prevents circular vehicle relationships.
     *
     * @param vehicle the vehicle to mount
     * @return {@code true} if the entity was mounted, or {@code false} if mounting was rejected
     */
    boolean mount(Entity vehicle);

    /**
     * Leaves the current vehicle, subject to exit-event cancellation.
     *
     * @param vehicle the vehicle this entity is riding
     * @return whether the entity left that vehicle
     */
    boolean dismount(Entity vehicle);

    /**
     * @return an immutable snapshot of direct passengers in seat order
     */
    List<? extends Entity> getPassengers();

    /**
     * Checks whether this entity carries the given direct passenger.
     */
    boolean hasPassenger(Entity passenger);

    /**
     * @return the direct passenger controlling this entity, or {@code null} when no passenger controls it
     */
    @Nullable
    Entity getControllingPassenger();

    /**
     * @param entity the entity to check
     * @return whether the entity controls this vehicle
     */
    boolean isControlling(Entity entity);

    /**
     * Checks whether the entity touches water or is exposed to rain.
     * Snowfall does not count as rain.
     *
     * @return whether water or rain is touching the entity
     */
    boolean isInWaterOrRain();

    /**
     * @return whether any burning duration remains
     */
    default boolean isOnFire() {
        return getFireTicks() > 0;
    }

    /**
     * Ignites the entity for the supplied duration, reduced by its fire protection.
     * Does not shorten an existing longer duration.
     *
     * @param seconds the non-negative duration before protection
     * @throws IllegalArgumentException if the duration is negative
     */
    void setOnFire(@NonNegative int seconds);

    /**
     * @return the remaining burning duration in ticks
     */
    @NonNegative
    int getFireTicks();

    /**
     * Removes fire and its visible burning effect.
     */
    void extinguish();

    /**
     * Returns the number of ticks this entity has been freezing.
     *
     * @return the current freeze ticks
     */
    int getFreezeTicks();

    /**
     * Sets the number of ticks this entity has been freezing.
     * Values are clamped between zero and {@link #getMaxFreezeTicks()}.
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

    /**
     * @return whether this entity is undead for potion and combat effects
     */
    default boolean isUndead() {
        return false;
    }

    /**
     * @return remaining ticks of temporary damage immunity
     */
    int getNoDamageTicks();

    /**
     * Sets temporary damage immunity, separate from the normal hurt cooldown.
     * Damage tagged to bypass invulnerability still applies.
     *
     * @param noDamageTicks non-negative immunity duration
     * @throws IllegalArgumentException if the duration is negative
     */
    void setNoDamageTicks(int noDamageTicks);
}
