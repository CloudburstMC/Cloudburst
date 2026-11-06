package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;

import java.util.Optional;

public interface BlockEntity {

    BlockEntityType<?> getType();

    Vector3i getPosition();

    Level getLevel();

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

    Block getBlock();

    BlockState getBlockState();

    void spawnToAll();

    void spawnTo(Player player);

    void scheduleUpdate();

    void setDirty();

    boolean onUpdate();

    void onBreak();

    /**
     * Returns the data stored for a plugin.
     *
     * @param pluginId the plugin id
     * @return the stored data, or an empty optional when none is stored
     */
    Optional<NbtMap> getPluginData(String pluginId);

    /**
     * Stores data for a plugin.
     *
     * @param pluginId the plugin id
     * @param data     the data to store
     */
    void setPluginData(String pluginId, NbtMap data);

    /**
     * Discards the data stored for a plugin.
     *
     * @param pluginId the plugin id
     */
    void removePluginData(String pluginId);
}
