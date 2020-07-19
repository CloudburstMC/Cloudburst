package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;

public interface Block {

    BlockState getState();

    Level getLevel();

    Chunk getChunk();

    Vector3i getPosition();

    default int getX() {
        return getPosition().getX();
    }

    default int getY() {
        return getPosition().getY();
    }

    default int getZ() {
        return getPosition().getZ();
    }

    int getLayer();

    BlockBehavior getBehaviour();

    default Block getSide(BlockFace face) {
        return getSide(face, 1);
    }

    Block getSide(BlockFace face, int step);

    Block getExtra();

    boolean isWaterlogged();
}
