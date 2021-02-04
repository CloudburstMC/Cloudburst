package org.cloudburstmc.api.blockentity;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;

import javax.annotation.Nullable;

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

    // boolean updateFromClient(NbtMap tag, Player player);

    Block getBlock();

    BlockState getBlockState();

    void spawnToAll();

    void spawnTo(Player player);

    void scheduleUpdate();

    void setDirty();

    boolean onUpdate();

    void onBreak();
}
