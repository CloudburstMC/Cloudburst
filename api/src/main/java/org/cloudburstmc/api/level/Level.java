package org.cloudburstmc.api.level;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.GameRuleMap;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;

import java.util.Map;
import java.util.Set;

public interface Level extends ChunkManager {
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

    Map<Long, ? extends Player> getPlayers();

    int getBiomeId(int x, int z);

    void setBiomeId(int x, int z, byte biomeId);

    int getHighestBlock(int x, int z);

    Vector3f getSpawnLocation();

    void setSpawnLocation(Vector3f pos);

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

    default AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox) {
        return getCollisionCubes(entity, boundingBox, true, false);
    }
    default AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox, boolean entities) {
        return getCollisionCubes(entity,boundingBox,entities,false);
    }

    AxisAlignedBB[] getCollisionCubes(Entity entity, AxisAlignedBB boundingBox, boolean entities, boolean solidEntites);

    void addEntityMovement(Entity entity, double x, double y, double z, double yaw, double pitch, double headYaw);

    void scheduleEntityUpdate(Entity entity);

    void removeEntity(Entity entity);

    Block[] getCollisionBlocks(AxisAlignedBB bb);

    default Set<? extends Entity> getCollidingEntities(AxisAlignedBB bb) {
        return getCollidingEntities(bb, null);
    }

    Set<? extends Entity> getCollidingEntities(AxisAlignedBB bb, Entity target);

    int getTickRate();

    boolean hasCollision(Entity entity, AxisAlignedBB bb, boolean entities);

    Entity getEntity(long runtimeId);

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

    Set<? extends Entity> getNearbyEntities(AxisAlignedBB bb);

    Set<? extends Entity> getNearbyEntities(AxisAlignedBB bb, Entity entity);

    Set<? extends Entity> getNearbyEntities(AxisAlignedBB bb, Entity entity, boolean loadChunks);

}
