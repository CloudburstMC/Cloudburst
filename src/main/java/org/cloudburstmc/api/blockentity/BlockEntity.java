package org.cloudburstmc.api.blockentity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;

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
}
