package org.cloudburstmc.server.testutil;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.boss.DragonBattle;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.ExplosionSettings;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.level.particle.ParticleEmission;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.HitResult;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * A level whose vertical bounds are concrete fields.
 *
 * <p>Dynamic proxies answer every call through reflective dispatch, which
 * dominates the per-cell cost of code reading through chunk levels in a
 * tight loop. The height checks on that path are concrete here, so a
 * benchmark comparing against such code measures the algorithm instead of
 * the proxy. Every other call behaves exactly like the wrapped level.
 */
public final class PlainTestLevel implements Level {

    private final Level delegate;
    private final int minHeight;
    private final int maxHeight;
    private final int sectionsCount;
    private final int minSectionY;
    private final Server server;
    private final long seed;

    private PlainTestLevel(Level delegate) {
        this.delegate = delegate;
        this.minHeight = delegate.getMinHeight();
        this.maxHeight = delegate.getMaxHeight();
        this.sectionsCount = delegate.getSectionsCount();
        this.minSectionY = delegate.getMinSectionY();
        this.server = delegate.getServer();
        this.seed = delegate.getSeed();
    }

    /**
     * Returns a level with the delegate's bounds and seed as concrete fields.
     *
     * @param delegate the level supplying every other call
     * @return the level
     */
    public static Level wrap(Level delegate) {
        return new PlainTestLevel(delegate);
    }

    @Override
    public int getMinHeight() {
        return this.minHeight;
    }

    @Override
    public int getMaxHeight() {
        return this.maxHeight;
    }

    @Override
    public int getSectionsCount() {
        return this.sectionsCount;
    }

    @Override
    public int getMinSectionY() {
        return this.minSectionY;
    }

    @Override
    public int getSectionIndex(int y) {
        return (y - this.minHeight) >> 4;
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return y < this.minHeight || y >= this.maxHeight;
    }

    @Override
    public Server getServer() {
        return this.server;
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public String getId() {
        return this.delegate.getId();
    }

    @Override
    public String getName() {
        return this.delegate.getName();
    }

    @Override
    public void setSeed(long seed) {
        this.delegate.setSeed(seed);
    }

    @Override
    public int getSeaLevel() {
        return this.delegate.getSeaLevel();
    }

    @Override
    public Vector3f getSpawnLocation() {
        return this.delegate.getSpawnLocation();
    }

    @Override
    public void setSpawnLocation(Vector3f position) {
        this.delegate.setSpawnLocation(position);
    }

    @Override
    public LevelGameRules getGameRules() {
        return this.delegate.getGameRules();
    }

    @Override
    public Difficulty getDifficulty() {
        return this.delegate.getDifficulty();
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        this.delegate.setDifficulty(difficulty);
    }

    @Override
    public @Nullable DragonBattle getDragonBattle() {
        return this.delegate.getDragonBattle();
    }

    @Override
    public boolean save(boolean force) {
        return this.delegate.save(force);
    }

    @Override
    public void scheduleUpdate(Vector3i position, int delay) {
        this.delegate.scheduleUpdate(position, delay);
    }

    @Override
    public boolean cancelScheduledUpdate(Vector3i position) {
        return this.delegate.cancelScheduledUpdate(position);
    }

    @Override
    public boolean isUpdateScheduled(Vector3i position) {
        return this.delegate.isUpdateScheduled(position);
    }

    @Override
    public void updateAround(Vector3i position) {
        this.delegate.updateAround(position);
    }

    @Override
    public void batchBlockUpdates(Runnable action) {
        this.delegate.batchBlockUpdates(action);
    }

    @Override
    public int getBiomeId(int x, int y, int z) {
        return this.delegate.getBiomeId(x, y, z);
    }

    @Override
    public void setBiomeId(int x, int y, int z, int biomeId) {
        this.delegate.setBiomeId(x, y, z, biomeId);
    }

    @Override
    public int getHighestBlock(int x, int z) {
        return this.delegate.getHighestBlock(x, z);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(Vector3i position) {
        return this.delegate.getBlockEntity(position);
    }

    @Override
    public boolean canBlockSeeSky(Vector3i position) {
        return this.delegate.canBlockSeeSky(position);
    }

    @Override
    public int getFullLight(Vector3i position) {
        return this.delegate.getFullLight(position);
    }

    @Override
    public float getLiquidHeight(Vector3i position) {
        return this.delegate.getLiquidHeight(position);
    }

    @Override
    public Vector3f getLiquidFlow(Vector3i position) {
        return this.delegate.getLiquidFlow(position);
    }

    @Override
    public boolean canSetLiquidState(Vector3i position, LiquidState liquid) {
        return this.delegate.canSetLiquidState(position, liquid);
    }

    @Override
    public boolean setLiquidState(Vector3i position, LiquidState liquid) {
        return this.delegate.setLiquidState(position, liquid);
    }

    @Override
    public boolean removeLiquid(Vector3i position) {
        return this.delegate.removeLiquid(position);
    }

    @Override
    public @Nullable ItemStack breakBlock(Vector3i position, @Nullable ItemStack item, @Nullable Player player,
            boolean createParticles) {
        return this.delegate.breakBlock(position, item, player, createParticles);
    }

    @Override
    public boolean explode(Vector3f position, ExplosionSettings settings) {
        return this.delegate.explode(position, settings);
    }

    @Override
    public Map<Long, ? extends Player> getPlayers() {
        return this.delegate.getPlayers();
    }

    @Override
    public Set<? extends Entity> getEntities() {
        return this.delegate.getEntities();
    }

    @Override
    public Set<? extends Entity> getNearbyEntities(BoundingBox boundingBox) {
        return this.delegate.getNearbyEntities(boundingBox);
    }

    @Override
    public Set<? extends Entity> getNearbyEntities(BoundingBox boundingBox,
            @Nullable Predicate<? super Entity> filter) {
        return this.delegate.getNearbyEntities(boundingBox, filter);
    }

    @Override
    public HitResult rayTraceBlocks(RayTraceContext context) {
        return this.delegate.rayTraceBlocks(context);
    }

    @Override
    public HitResult rayTraceEntities(Vector3f start, Vector3f end, float raySize,
            Predicate<? super Entity> filter) {
        return this.delegate.rayTraceEntities(start, end, raySize, filter);
    }

    @Override
    public HitResult rayTrace(RayTraceContext context, float raySize, Predicate<? super Entity> entityFilter) {
        return this.delegate.rayTrace(context, raySize, entityFilter);
    }

    @Override
    public boolean hasCollision(@Nullable Entity entity, BoundingBox boundingBox, boolean includeEntities) {
        return this.delegate.hasCollision(entity, boundingBox, includeEntities);
    }

    @Override
    public boolean hasBlockCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.delegate.hasBlockCollision(entity, boundingBox);
    }

    @Override
    public boolean hasEntityCollision(@Nullable Entity entity, BoundingBox boundingBox) {
        return this.delegate.hasEntityCollision(entity, boundingBox);
    }

    @Override
    public boolean hasEntityCollision(@Nullable Entity entity, VoxelShape shape, Vector3i position) {
        return this.delegate.hasEntityCollision(entity, shape, position);
    }

    @Override
    public boolean hasPlacementObstruction(VoxelShape shape, Vector3i position) {
        return this.delegate.hasPlacementObstruction(shape, position);
    }

    @Override
    public DroppedItem dropItem(Vector3f position, ItemStack item, @Nullable Vector3f motion, boolean dropAround,
            int delay) {
        return this.delegate.dropItem(position, item, motion, dropAround, delay);
    }

    @Override
    public long getTime() {
        return this.delegate.getTime();
    }

    @Override
    public void setTime(long time) {
        this.delegate.setTime(time);
    }

    @Override
    public long getFullTime() {
        return this.delegate.getFullTime();
    }

    @Override
    public void setFullTime(long time) {
        this.delegate.setFullTime(time);
    }

    @Override
    public long getGameTime() {
        return this.delegate.getGameTime();
    }

    @Override
    public boolean hasDaylightCycle() {
        return this.delegate.hasDaylightCycle();
    }

    @Override
    public boolean isDayTime() {
        return this.delegate.isDayTime();
    }

    @Override
    public boolean isNightTime() {
        return this.delegate.isNightTime();
    }

    @Override
    public boolean isTimePaused() {
        return this.delegate.isTimePaused();
    }

    @Override
    public void setTimePaused(boolean paused) {
        this.delegate.setTimePaused(paused);
    }

    @Override
    public float getTimeRate() {
        return this.delegate.getTimeRate();
    }

    @Override
    public void setTimeRate(float rate) {
        this.delegate.setTimeRate(rate);
    }

    @Override
    public boolean isRaining() {
        return this.delegate.isRaining();
    }

    @Override
    public boolean setRaining(boolean raining) {
        return this.delegate.setRaining(raining);
    }

    @Override
    public int getRainTime() {
        return this.delegate.getRainTime();
    }

    @Override
    public void setRainTime(int time) {
        this.delegate.setRainTime(time);
    }

    @Override
    public boolean isThundering() {
        return this.delegate.isThundering();
    }

    @Override
    public boolean strikeLightning(Vector3f position) {
        return this.delegate.strikeLightning(position);
    }

    @Override
    public boolean setThundering(boolean thundering) {
        return this.delegate.setThundering(thundering);
    }

    @Override
    public int getThunderTime() {
        return this.delegate.getThunderTime();
    }

    @Override
    public void setThunderTime(int time) {
        this.delegate.setThunderTime(time);
    }

    @Override
    public void sendWeather(Player... players) {
        this.delegate.sendWeather(players);
    }

    @Override
    public void playSound(Vector3f position, SoundPlayback playback) {
        this.delegate.playSound(position, playback);
    }

    @Override
    public void playSound(Vector3f position, SoundPlayback playback, Player... players) {
        this.delegate.playSound(position, playback, players);
    }

    @Override
    public void playBlockBreakEffect(Vector3i position, BlockState state) {
        this.delegate.playBlockBreakEffect(position, state);
    }

    @Override
    public void spawnParticle(ParticleEmission emission, Vector3f position) {
        this.delegate.spawnParticle(emission, position);
    }

    @Override
    public void spawnParticle(ParticleEmission emission, Vector3f position, Player... players) {
        this.delegate.spawnParticle(emission, position, players);
    }

    @Override
    public void spawnParticleEffect(ParticleEmitter emitter, Vector3f position) {
        this.delegate.spawnParticleEffect(emitter, position);
    }

    @Override
    public void spawnParticleEffect(ParticleEmitter emitter, Vector3f position, Player... players) {
        this.delegate.spawnParticleEffect(emitter, position, players);
    }

    @Override
    public BlockState getBlockState(int x, int y, int z, BlockLayer layer) {
        return this.delegate.getBlockState(x, y, z, layer);
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return this.delegate.getBlock(x, y, z);
    }

    @Override
    public @Nullable Block getLoadedBlock(int x, int y, int z) {
        return this.delegate.getLoadedBlock(x, y, z);
    }

    @Override
    public boolean setBlockState(int x, int y, int z, BlockLayer layer, BlockState state, boolean direct,
            boolean update) {
        return this.delegate.setBlockState(x, y, z, layer, state, direct, update);
    }

    @Override
    public Chunk getChunk(long key) {
        return this.delegate.getChunk(key);
    }

    @Override
    public @Nullable Chunk getLoadedChunk(long key) {
        return this.delegate.getLoadedChunk(key);
    }

    @Override
    public CompletableFuture<? extends Chunk> getChunkFuture(int chunkX, int chunkZ) {
        return this.delegate.getChunkFuture(chunkX, chunkZ);
    }

    @Override
    public Set<? extends Chunk> getChunks() {
        return this.delegate.getChunks();
    }

    @Override
    public Set<? extends Player> getChunkPlayers(int chunkX, int chunkZ) {
        return this.delegate.getChunkPlayers(chunkX, chunkZ);
    }
}
