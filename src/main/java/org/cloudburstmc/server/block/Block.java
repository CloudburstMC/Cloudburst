package org.cloudburstmc.server.block;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.Direction;

import static org.cloudburstmc.server.math.Direction.*;

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

    default Block up(int step) {
        return getSide(UP, step);
    }

    default Block down() {
        return getSide(DOWN, 1);
    }

    default Block down(int step) {
        return getSide(DOWN, step);
    }

    default Block north() {
        return getSide(NORTH, 1);
    }

    default Block north(int step) {
        return getSide(NORTH, step);
    }

    default Block east() {
        return getSide(EAST, 1);
    }

    default Block east(int step) {
        return getSide(EAST, step);
    }

    default Block south() {
        return getSide(SOUTH, 1);
    }

    default Block south(int step) {
        return getSide(SOUTH, step);
    }

    default Block west() {
        return getSide(WEST, 1);
    }

    default Block west(int step) {
        return getSide(WEST, step);
    }

    default Block getSide(Direction face) {
        return getSide(face, 1);
    }

    Block getSide(Direction face, int step);

    Block getRelative(int x, int y, int z);

    boolean isWaterlogged();

    int getLayer();

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
