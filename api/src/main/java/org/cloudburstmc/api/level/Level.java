package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.boss.DragonBattle;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents a loaded level and provides access to its blocks, entities, rules, time, and weather.
 *
 * <p>Unless a method explicitly refers to loaded data, block and chunk access may load or generate chunks.
 */
public interface Level extends ChunkManager, LevelHeightAccessor {
    /**
     * Returns the server that owns this level.
     *
     * @return the server
     */
    Server getServer();

    /**
     * Returns the stable identifier used to locate this level's stored data.
     *
     * @return the level identifier
     */
    String getId();

    /**
     * Returns the level's display name.
     *
     * @return the level name
     */
    String getName();

    /**
     * Returns the seed used for generation.
     *
     * @return the level seed
     */
    long getSeed();

    /**
     * Changes the seed used for future generation.
     *
     * @param seed the new level seed
     */
    void setSeed(long seed);

    /**
     * Returns the sea level used by this level's terrain generator.
     *
     * @return sea level
     */
    int getSeaLevel();

    /**
     * Returns the default spawn position.
     *
     * @return the spawn position
     */
    Vector3f getSpawnLocation();

    /**
     * Changes the default spawn position.
     *
     * @param position the new spawn position
     */
    void setSpawnLocation(Vector3f position);

    /**
     * Returns the game rules for this level.
     *
     * @return the level game rules
     */
    LevelGameRules getGameRules();

    /**
     * Returns this level's difficulty.
     *
     * @return the level difficulty
     */
    Difficulty getDifficulty();

    /**
     * Changes this level's difficulty.
     *
     * @param difficulty the new difficulty
     */
    void setDifficulty(Difficulty difficulty);

    /**
     * Returns the dragon battle associated with this level.
     *
     * @return the dragon battle, or {@code null} when this is not an End level
     */
    @Nullable
    DragonBattle getDragonBattle();

    /**
     * Returns the server tick currently being processed by this level.
     *
     * @return the current tick
     */
    long getCurrentTick();

    /**
     * Saves changed level and chunk data.
     *
     * @return {@code true} when saving was started
     */
    default boolean save() {
        return save(false);
    }

    /**
     * Saves level and chunk data.
     *
     * @param force whether to save when automatic saving is disabled
     * @return {@code true} when saving was started
     */
    boolean save(boolean force);

    /**
     * Schedules a block update after a delay.
     *
     * @param position the block position
     * @param delay    the delay in ticks
     */
    void scheduleUpdate(Vector3i position, int delay);

    /**
     * Cancels the scheduled block update at a position.
     *
     * @param position the block position
     * @return {@code true} when an update was cancelled
     */
    boolean cancelScheduledUpdate(Vector3i position);

    /**
     * Checks whether a block update is scheduled at a position.
     *
     * @param position the block position
     * @return {@code true} when an update is scheduled
     */
    boolean isUpdateScheduled(Vector3i position);

    /**
     * Notifies the blocks neighboring a position of a block change.
     *
     * @param position the changed block position
     */
    void updateAround(Vector3i position);

    /**
     * Returns the biome ID at a position.
     *
     * @param x the block X coordinate
     * @param y the block Y coordinate
     * @param z the block Z coordinate
     * @return the biome ID
     */
    int getBiomeId(int x, int y, int z);

    /**
     * Changes the biome at a position.
     *
     * @param x       the block X coordinate
     * @param y       the block Y coordinate
     * @param z       the block Z coordinate
     * @param biomeId the biome ID
     */
    void setBiomeId(int x, int y, int z, int biomeId);

    /**
     * Returns the highest non-air block in a column.
     *
     * @param x the block X coordinate
     * @param z the block Z coordinate
     * @return the block Y coordinate, or {@code -1} when the column is empty
     */
    int getHighestBlock(int x, int z);

    /**
     * Returns the block entity at a position.
     *
     * @param position the block position
     * @return the block entity, or {@code null} when none is present
     */
    @Nullable
    BlockEntity getBlockEntity(Vector3i position);

    /**
     * Checks whether a position has an unobstructed view of the sky.
     *
     * @param position the block position
     * @return {@code true} when the position can see the sky
     */
    boolean canBlockSeeSky(Vector3i position);

    /**
     * Returns the greater of sky and block light at a position.
     *
     * @param position the block position
     * @return the light level from {@code 0} to {@code 15}
     */
    int getFullLight(Vector3i position);

    /**
     * Returns the liquid in either block layer.
     *
     * @param position the block position
     * @return the liquid state, or the empty liquid state when none is present
     */
    default LiquidState getLiquidState(Vector3i position) {
        return getBlock(position).getLiquid();
    }

    /**
     * Returns the liquid surface height at a block position.
     *
     * @param position the block position
     * @return the height from {@code 0} for no liquid to {@code 1} for a full column
     */
    float getLiquidHeight(Vector3i position);

    /**
     * Returns the direction in which liquid at a block position is flowing.
     *
     * @param position the block position
     * @return the normalized flow vector, or the zero vector when there is no flow
     */
    Vector3f getLiquidFlow(Vector3i position);

    /**
     * Checks whether liquid can occupy the primary or secondary block layer.
     *
     * @param position the block position
     * @param liquid   the liquid state
     * @return whether the liquid can be placed
     */
    boolean canSetLiquidState(Vector3i position, LiquidState liquid);

    /**
     * Places liquid in the primary layer or in a block state that can contain it.
     *
     * @param position the block position
     * @param liquid   the liquid state to place
     * @return whether the liquid was placed
     */
    boolean setLiquidState(Vector3i position, LiquidState liquid);

    /**
     * Removes liquid from either block layer without removing its container.
     *
     * @param position the block position
     * @return whether liquid was removed
     */
    boolean removeLiquid(Vector3i position);

    /**
     * Breaks a block without a player or tool.
     *
     * @param position the block position
     * @return the resulting tool stack, or {@code null} if the block was not broken
     */
    default @Nullable ItemStack breakBlock(Vector3i position) {
        return this.breakBlock(position, null);
    }

    /**
     * Breaks a block using a tool without a player.
     *
     * @param position the block position
     * @param item     the tool, or {@code null} for an empty hand
     * @return the resulting tool stack, or {@code null} if the block was not broken
     */
    default @Nullable ItemStack breakBlock(Vector3i position, @Nullable ItemStack item) {
        return this.breakBlock(position, item, null);
    }

    /**
     * Breaks a block on behalf of a player.
     *
     * @param position the block position
     * @param item     the tool, or {@code null} for an empty hand
     * @param player   the player, or {@code null} for no player
     * @return the resulting tool stack, or {@code null} if the block was not broken
     */
    default @Nullable ItemStack breakBlock(Vector3i position, @Nullable ItemStack item, @Nullable Player player) {
        return this.breakBlock(position, item, player, false);
    }

    /**
     * Breaks a block on behalf of a player.
     *
     * @param position        the block position
     * @param item            the tool, or {@code null} for an empty hand
     * @param player          the player, or {@code null} for no player
     * @param createParticles whether to send block-break particles
     * @return the resulting tool stack, or {@code null} if the block was not broken
     */
    @Nullable
    ItemStack breakBlock(Vector3i position, @Nullable ItemStack item, @Nullable Player player, boolean createParticles);

    /**
     * Returns the players currently present in this level, keyed by unique entity ID.
     *
     * @return the players in this level
     */
    Map<Long, ? extends Player> getPlayers();

    /**
     * Returns the entities currently loaded in this level.
     *
     * @return loaded entities
     */
    Set<? extends Entity> getEntities();

    /**
     * Gets entities whose bounding boxes intersect the supplied box.
     *
     * @param boundingBox the search box
     * @return the matching entities
     */
    Set<? extends Entity> getNearbyEntities(BoundingBox boundingBox);

    /**
     * Gets entities whose bounding boxes intersect the supplied box and match a filter.
     *
     * @param boundingBox the search box
     * @param filter      the entity filter, or {@code null} to include all nearby entities
     * @return the matching entities
     */
    Set<? extends Entity> getNearbyEntities(BoundingBox boundingBox, @Nullable Predicate<? super Entity> filter);

    /**
     * Tests whether a bounding box collides with blocks or entities in this level.
     *
     * @param boundingBox the box to test
     * @return {@code true} if the box collides
     */
    default boolean hasCollision(BoundingBox boundingBox) {
        return this.hasCollision(null, boundingBox);
    }

    /**
     * Tests whether an entity's current bounding box collides with blocks or other entities.
     *
     * @param entity the entity to test
     * @return {@code true} if the entity collides
     */
    default boolean hasCollision(Entity entity) {
        return this.hasCollision(entity, entity.getBoundingBox());
    }

    /**
     * Tests whether a bounding box collides with blocks or entities in this level.
     *
     * <p>The supplied entity is excluded from entity collision checks.</p>
     *
     * @param entity      the entity being tested, or {@code null}
     * @param boundingBox the box to test
     * @return {@code true} if the box collides
     */
    default boolean hasCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.hasCollision(entity, boundingBox, true);
    }

    /**
     * Tests whether a bounding box collides in this level.
     *
     * @param entity          the entity being tested, or {@code null}
     * @param boundingBox     the box to test
     * @param includeEntities whether entity collisions should be included
     * @return {@code true} if the box collides
     */
    boolean hasCollision(@Nullable Entity entity, BoundingBox boundingBox, boolean includeEntities);

    /**
     * Tests whether a bounding box collides with block collision shapes.
     *
     * @param entity      the entity being tested, or {@code null}
     * @param boundingBox the box to test
     * @return {@code true} if any block collision shape overlaps the box
     */
    boolean hasBlockCollision(@Nullable Entity entity, BoundingBox boundingBox);

    /**
     * Tests whether a bounding box collides with collidable entities.
     *
     * @param entity      the entity being tested, or {@code null}
     * @param boundingBox the box to test
     * @return {@code true} if any entity collision shape overlaps the box
     */
    boolean hasEntityCollision(@Nullable Entity entity, BoundingBox boundingBox);

    /**
     * Tests whether a block-local shape collides with collidable entities.
     *
     * @param entity   the entity being tested, or {@code null}
     * @param shape    the shape in block-local coordinates
     * @param position the block position where the shape is placed
     * @return {@code true} if any entity collision shape overlaps the placed shape
     */
    boolean hasEntityCollision(@Nullable Entity entity, VoxelShape shape, Vector3i position);

    /**
     * Drops an item at the center of a block position.
     *
     * @param position the block position
     * @param item     the item to drop
     * @return the created dropped-item entity
     */
    default DroppedItem dropItem(Vector3i position, ItemStack item) {
        return dropItem(position.toFloat().add(0.5f, 0f, 0.5f), item);
    }

    /**
     * Drops an item at a position using default motion and pickup delay.
     *
     * @param position the drop position
     * @param item     the item to drop
     * @return the created dropped-item entity
     */
    default DroppedItem dropItem(Vector3f position, ItemStack item) {
        return dropItem(position, item, null);
    }

    /**
     * Drops an item at a position.
     *
     * @param position the drop position
     * @param item     the item to drop
     * @param motion   the initial motion, or {@code null} for default motion
     * @return the created dropped-item entity
     */
    default DroppedItem dropItem(Vector3f position, ItemStack item, @Nullable Vector3f motion) {
        return dropItem(position, item, motion, false);
    }

    /**
     * Drops an item at a position with a pickup delay.
     *
     * @param position the drop position
     * @param item     the item to drop
     * @param motion   the initial motion, or {@code null} for default motion
     * @param delay    the pickup delay in ticks
     * @return the created dropped-item entity
     */
    default DroppedItem dropItem(Vector3f position, ItemStack item, @Nullable Vector3f motion, int delay) {
        return dropItem(position, item, motion, false, delay);
    }

    /**
     * Drops an item at a position using the default pickup delay.
     *
     * @param position   the drop position
     * @param item       the item to drop
     * @param motion     the initial motion, or {@code null} for default motion
     * @param dropAround whether default motion should spread outward
     * @return the created dropped-item entity
     */
    default DroppedItem dropItem(Vector3f position, ItemStack item, @Nullable Vector3f motion, boolean dropAround) {
        return dropItem(position, item, motion, dropAround, 10);
    }

    /**
     * Drops an item with complete control over its initial placement and pickup delay.
     *
     * @param position   the drop position
     * @param item       the item to drop
     * @param motion     the initial motion, or {@code null} for default motion
     * @param dropAround whether default motion should spread outward
     * @param delay      the pickup delay in ticks
     * @return the created dropped-item entity
     */
    DroppedItem dropItem(Vector3f position, ItemStack item, @Nullable Vector3f motion, boolean dropAround, int delay);

    /**
     * Returns the elapsed daylight-cycle time.
     *
     * <p>The value is not limited to a single day.
     *
     * @return the elapsed time in ticks
     */
    long getTime();

    /**
     * Changes the elapsed daylight-cycle time.
     *
     * @param time the time in ticks
     */
    void setTime(long time);

    /**
     * Resumes the passage of time.
     */
    void startTime();

    /**
     * Pauses the passage of time.
     */
    void stopTime();

    /**
     * Sends this level's current time to a player.
     *
     * @param who the recipient
     */
    default void sendTime(Player who) {
        sendTime(new Player[]{who});
    }

    /**
     * Sends this level's current time to the supplied players.
     *
     * @param players the recipients
     */
    void sendTime(Player... players);

    /**
     * Checks whether rain is active.
     *
     * @return {@code true} when it is raining
     */
    boolean isRaining();

    /**
     * Starts or stops rain.
     *
     * @param raining whether rain should be active
     * @return {@code true} when the change was accepted
     */
    boolean setRaining(boolean raining);

    /**
     * Returns the remaining rain duration.
     *
     * @return the remaining duration in ticks
     */
    int getRainTime();

    /**
     * Changes the remaining rain duration.
     *
     * @param time the duration in ticks
     */
    void setRainTime(int time);

    /**
     * Checks whether a thunderstorm is active.
     *
     * @return {@code true} when it is thundering
     */
    boolean isThundering();

    /**
     * Starts or stops thunder.
     *
     * @param thundering whether thunder should be active
     * @return {@code true} when the change was accepted
     */
    boolean setThundering(boolean thundering);

    /**
     * Returns the remaining thunder duration.
     *
     * @return the remaining duration in ticks
     */
    int getThunderTime();

    /**
     * Changes the remaining thunder duration.
     *
     * @param time the duration in ticks
     */
    void setThunderTime(int time);

    /**
     * Sends this level's current weather to the supplied players.
     *
     * @param players the recipients
     */
    void sendWeather(Player... players);

    /**
     * Spawns a particle at a position for players tracking the surrounding chunk.
     *
     * @param particle the particle to spawn
     * @param position the particle position
     */
    void spawnParticle(ParticleType particle, Vector3f position);

    /**
     * Spawns a particle at a position for specific players.
     *
     * @param particle the particle to spawn
     * @param position the particle position
     * @param players  the players to receive the particle
     */
    void spawnParticle(ParticleType particle, Vector3f position, Player... players);
}
