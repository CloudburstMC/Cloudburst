package org.cloudburstmc.api.level;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.math.vector.Vector4i;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.gamerule.GameRuleMap;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface Level {
    int BLOCK_UPDATE_NORMAL = 1;
    int BLOCK_UPDATE_RANDOM = 2;
    int BLOCK_UPDATE_SCHEDULED = 3;
    int BLOCK_UPDATE_WEAK = 4;
    int BLOCK_UPDATE_TOUCH = 5;
    int BLOCK_UPDATE_REDSTONE = 6;
    int BLOCK_UPDATE_TICK = 7;

    void init();

    Server getServer();

    String getId();

    void close();

    default boolean unload() {
        return unload(false);
    }

    boolean unload(boolean force);

    Set<Player> getChunkPlayers(int chunkX, int chunkZ);

    GameRuleMap getGameRules();

    void doTick(int currentTick);

    default boolean save() {
        return save(false);
    }

    boolean save(boolean force);

    void scheduleUpdate(Vector3i pos, int delay);

    void updateAround(Vector3i pos);

    boolean cancelScheduledUpdate(Vector3i pos);

    boolean isUpdateScheduled(Vector3i pos);

    int getFullLight(Vector3i pos);

    default boolean setBlockAt(Vector3i pos, BlockState blockState) {
        return this.setBlockAt(pos, blockState, false);
    }

    default boolean setBlockAt(Vector3i pos, int layer, BlockState blockState) {
        return this.setBlockAt(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, false, true);
    }

    default boolean setBlockAt(Vector3i pos, BlockState blockState, boolean direct) {
        return this.setBlockAt(pos, blockState, direct, true);
    }

    default boolean setBlockAt(Vector3i pos, int layer, BlockState blockState, boolean direct, boolean update) {
        return setBlockAt(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, direct, update);
    }

    default boolean setBlockAt(Vector3i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlockAt(pos.getX(), pos.getY(), pos.getZ(), 0, blockState, direct, update);
    }

    default boolean setBlockAt(Vector4i pos, BlockState blockState) {
        return this.setBlockAt(pos, blockState, false);
    }

    default boolean setBlockAt(Vector4i pos, BlockState blockState, boolean direct) {
        return this.setBlockAt(pos, blockState, direct, true);
    }

    default boolean setBlockAt(Vector4i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlockAt(pos.getX(), pos.getY(), pos.getZ(), pos.getW(), blockState, direct, update);
    }

    default boolean setBlockAt(int x, int y, int z, BlockState state) {
        return setBlockAt(x,y,z,0,state,false,true);
    }

    default boolean setBlockAt(int x, int y, int z, int layer, BlockState state) {
        return setBlockAt(x,y,z,layer,state,false,true);
    }

    boolean setBlockAt(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update);

    default ItemStack useBreakOn(Vector3i pos) {
        return this.useBreakOn(pos, null);
    }

    default ItemStack useBreakOn(Vector3i pos, ItemStack item) {
        return this.useBreakOn(pos, item, null);
    }

    default ItemStack useBreakOn(Vector3i pos, ItemStack item, Player player) {
        return this.useBreakOn(pos, item, player, false);
    }

    default ItemStack useBreakOn(Vector3i pos, ItemStack item, Player player, boolean createParticles) {
        return useBreakOn(pos, null, item, player, createParticles);
    }

    ItemStack useBreakOn(Vector3i pos, Direction face, ItemStack item, Player player, boolean createParticles);

    default ItemStack useItemOn(Vector3i vector, ItemStack item, Direction face, Vector3f clickPos) {
        return this.useItemOn(vector, item, face, clickPos, null);
    }

    default ItemStack useItemOn(Vector3i vector, ItemStack item, Direction face, Vector3f clickPos, Player player) {
        return this.useItemOn(vector, item, face, clickPos, player, true);
    }

    ItemStack useItemOn(Vector3i vector, ItemStack item, Direction face, Vector3f clickPos, Player player, boolean playSound);

    Map<Long, Player> getPlayers();

    default BlockState getBlockState(Vector4i pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ(), pos.getW());
    }

    default BlockState getBlockState(Vector3i pos) {
        return getBlockState(pos, 0);
    }

    default BlockState getBlockState(Vector3i pos, int layer) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ(), layer);
    }
    
    default BlockState getBlockState(int x, int y, int z) {
        return getBlockState(x, y, z, 0);
    }

    BlockState getBlockState(int x, int y, int z, int layer);

    int getBiomeId(int x, int z);

    void setBiomeId(int x, int z, byte biomeId);

    int getHighestBlock(int x, int z);

    Vector3f getSpawnLocation();

    void setSpawnLocation(Vector3f pos);

    int getTime();

    void setTime();

    void stopTime();

    void startTime();

    long getCurrentTick();

    String getName();

    long getSeed();

    void setSeed();

    boolean isRaining();

    void setRaining(boolean raining);

    int getRainTime();

    void setRainTime();

    boolean isThundering();

    void setThundering();

    int getThunderTime();

    void setThunderTime();

    void sendWeather(Player... players);

    Chunk getChunk(int chunkX, int chunkZ);

    Chunk getLoadedChunk(Vector3f position);

    CompletableFuture<Chunk> getChunkFuture(int chunkX, int chunkZ);

    void addEntity(Entity entity);

    default AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox) {
        return getCollisionCubes(entity, boundingBox, true, false);
    }
    default AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox, boolean entities) {
        return getCollisionCubes(entity,boundingBox,entities,false);
    }

    AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox, boolean entities, boolean solidEntites);

    void addEntityMovement(Entity entity, double x, double y, double z, double yaw, double pitch, double headYaw);

    void scheduleEntityUpdate(Entity entity);

    default Block getBlock(Vector3i pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    Block getBlock(int x, int y, int z);

    void removeEntity(Entity entity);

    Block getLoadedBlock(int x, int y, int z);

    Block[] getCollisionBlocks(AxisAlignedBB bb);

    Entity[] getCollidingEntities(AxisAlignedBB bb, Entity target);

    int getTickRate();

    boolean hasCollision(Entity entity, AxisAlignedBB bb, boolean entities);

    Entity getEntity(long runtimeId);

    BlockEntity getBlockEntity(Vector3i position);

    boolean canBlockSeeSky(Vector3i position);
}
