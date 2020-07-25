package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;

import static org.cloudburstmc.server.math.BlockFace.*;

public interface Block extends BlockSnapshot {

    BlockSnapshot snapshot();

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

    default Block up() {
        return getSide(UP, 1);
    }

    default Block down() {
        return getSide(DOWN, 1);
    }

    default Block north() {
        return getSide(NORTH, 1);
    }

    default Block east() {
        return getSide(EAST, 1);
    }

    default Block south() {
        return getSide(SOUTH, 1);
    }

    default Block west() {
        return getSide(WEST, 1);
    }

    default Block getSide(BlockFace face) {
        return getSide(face, 1);
    }

    Block getSide(BlockFace face, int step);

    boolean isWaterlogged();

    default void set(BlockState state) {
        this.set(state, 0, false, true);
    }

    default void set(BlockState state, boolean direct) {
        this.set(state, 0, direct, true);
    }

    default void set(BlockState state, boolean direct, boolean update) {
        this.set(state, 0, direct, update);
    }

    default void setExtra(BlockState state) {
        this.set(state, 1, false, true);
    }

    default void setExtra(BlockState state, boolean direct) {
        this.set(state, 1, direct, true);
    }

    default void setExtra(BlockState state, boolean direct, boolean update) {
        this.set(state, 1, direct, update);
    }

    void set(BlockState state, int layer, boolean direct, boolean update);
}
