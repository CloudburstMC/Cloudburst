package org.cloudburstmc.api.level;

import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4i;

import java.util.Map;
import java.util.Set;

public interface Level {
    void init();

    Server getServer();

    String getId();

    void close();

    default boolean unload() {
        return unload(false);
    }

    boolean unload(boolean force);

    Set<Player> getChunkPlayers(int chunkX, int chunkZ);

    Set<GameRule<?>> getGameRules();

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

    default boolean setBlock(Vector3i pos, BlockState blockState) {
        return this.setBlock(pos, blockState, false);
    }

    default boolean setBlock(Vector3i pos, int layer, BlockState blockState) {
        return this.setBlock(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, false, true);
    }

    default boolean setBlock(Vector3i pos, BlockState blockState, boolean direct) {
        return this.setBlock(pos, blockState, direct, true);
    }

    default boolean setBlock(Vector3i pos, int layer, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), layer, blockState, direct, update);
    }

    default boolean setBlock(Vector3i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), 0, blockState, direct, update);
    }

    default boolean setBlock(Vector4i pos, BlockState blockState) {
        return this.setBlock(pos, blockState, false);
    }

    default boolean setBlock(Vector4i pos, BlockState blockState, boolean direct) {
        return this.setBlock(pos, blockState, direct, true);
    }

    default boolean setBlock(Vector4i pos, BlockState blockState, boolean direct, boolean update) {
        return setBlock(pos.getX(), pos.getY(), pos.getZ(), pos.getW(), blockState, direct, update);
    }

    boolean setBlock(int x, int y, int z, int layer, BlockState state, boolean direct, boolean update);

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

    default BlockState getBlockAt(Vector4i pos) {
        return getBlockAt(pos.getX(), pos.getY(), pos.getZ(), pos.getW());
    }

    default BlockState getBlockAt(Vector3i pos) {
        return getBlockAt(pos, 0);
    }

    default BlockState getBlockAt(Vector3i pos, int layer) {
        return getBlockAt(pos.getX(), pos.getY(), pos.getZ(), layer);
    }

    BlockState getBlockAt(int x, int y, int z, int layer);

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
}
