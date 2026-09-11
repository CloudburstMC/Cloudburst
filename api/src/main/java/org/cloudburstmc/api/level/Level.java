package org.cloudburstmc.api.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.blockentity.BlockEntity;
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

public interface Level extends ChunkManager, LevelHeightAccessor {
    int BLOCK_UPDATE_NORMAL = 1;
    int BLOCK_UPDATE_RANDOM = 2;
    int BLOCK_UPDATE_SCHEDULED = 3;
    int BLOCK_UPDATE_WEAK = 4;
    int BLOCK_UPDATE_TOUCH = 5;
    int BLOCK_UPDATE_REDSTONE = 6;
    int BLOCK_UPDATE_TICK = 7;

    int TIME_DAY = 0;
    int TIME_NOON = 6000;
    int TIME_SUNSET = 12000;
    int TIME_NIGHT = 14000;
    int TIME_MIDNIGHT = 18000;
    int TIME_SUNRISE = 23000;
    int TIME_FULL = 24000;

    void init();

    Server getServer();

    String getId();

    void close();

    default boolean unload() {
        return unload(false);
    }

    boolean unload(boolean force);

    LevelGameRules getGameRules();

    void doTick(int currentTick);

    default boolean save() {
        return save(false);
    }

    boolean save(boolean force);

    void scheduleUpdate(Vector3i position, int delay);

    void updateAround(Vector3i position);

    boolean cancelScheduledUpdate(Vector3i position);

    boolean isUpdateScheduled(Vector3i position);

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

    int getFullLight(Vector3i position);

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

    Map<Long, ? extends Player> getPlayers();

    /**
     * Returns the entities currently loaded in this level.
     *
     * @return loaded entities
     */
    Set<? extends Entity> getEntities();

    int getBiomeId(int x, int y, int z);

    void setBiomeId(int x, int y, int z, int biomeId);

    int getHighestBlock(int x, int z);

    Vector3f getSpawnLocation();

    void setSpawnLocation(Vector3f position);

    int getTime();

    void setTime(int time);

    void stopTime();

    void startTime();

    default void sendTime(Player who) {
        sendTime(new Player[]{who});
    }

    void sendTime(Player... players);

    long getCurrentTick();

    String getName();

    long getSeed();

    void setSeed(long seed);

    boolean isRaining();

    boolean setRaining(boolean raining);

    int getRainTime();

    void setRainTime(int time);

    boolean isThundering();

    boolean setThundering(boolean thundering);

    int getThunderTime();

    void setThunderTime(int time);

    void sendWeather(Player... players);

    void addEntity(Entity entity);

    void addEntityMovement(Entity entity, double x, double y, double z, double yaw, double pitch, double headYaw);

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

    void scheduleEntityUpdate(Entity entity);

    void removeEntity(Entity entity);

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

    BlockEntity getBlockEntity(Vector3i position);

    boolean canBlockSeeSky(Vector3i position);

    default DroppedItem dropItem(Vector3i position, ItemStack item) {
        return dropItem(position.toFloat().add(0.5f, 0f, 0.5f), item);
    }

    default DroppedItem dropItem(Vector3f position, ItemStack item) {
        return dropItem(position, item, null);
    }

    default DroppedItem dropItem(Vector3f position, ItemStack item, Vector3f motion) {
        return dropItem(position, item, motion, false);
    }

    default DroppedItem dropItem(Vector3f position, ItemStack item, Vector3f motion, int delay) {
        return dropItem(position, item, motion, false, delay);
    }

    default DroppedItem dropItem(Vector3f position, ItemStack item, Vector3f motion, boolean dropAround) {
        return dropItem(position, item, motion, dropAround, 10);
    }

    DroppedItem dropItem(Vector3f position, ItemStack item, Vector3f motion, boolean dropAround, int delay);

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

}
