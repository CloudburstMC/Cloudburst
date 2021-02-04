package org.cloudburstmc.api.blockentity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.player.Player;

import javax.annotation.Nullable;

public interface BlockEntity {

    BlockEntityType<?> getType();

    Vector3i getPosition();

    Level getLevel();

    void loadAdditionalData(NbtMap tag);

    void saveAdditionalData(NbtMapBuilder tag);

    /**
     * Gets the NBT for items with block entity data
     * Contains all server side NBT without ID and position
     *
     * @return block entity tag
     */
    NbtMap getItemTag();

    /**
     * Get the NBT for saving the block entity to disk
     * Contains all server side NBT with ID and position
     *
     * @return block entity tag
     */
    NbtMap getServerTag();

    /**
     * Get the NBT for saving sending to the client in {@link BlockEntityDataPacket}
     * Contains all server side NBT with ID but no position
     *
     * @return block entity tag
     */
    NbtMap getClientTag();

    /**
     * Gets the block entity NBT that is sent in a chunk packet
     * Contains no server side NBT
     *
     * @return block entity tag
     */
    NbtMap getChunkTag();

    boolean isValid();

    boolean isClosed();

    void close();

    boolean isMovable();

    void setMovable(boolean movable);

    @Nullable
    String getCustomName();

    void setCustomName(@Nullable String customName);

    boolean hasCustomName();

    boolean isSpawnable();

    boolean updateFromClient(NbtMap tag, Player player);

    Block getBlock();

    BlockState getBlockState();

    void spawnToAll();

    void spawnTo(Player player);

    void scheduleUpdate();

    void setDirty();

    boolean onUpdate();

    void onBreak();
}
