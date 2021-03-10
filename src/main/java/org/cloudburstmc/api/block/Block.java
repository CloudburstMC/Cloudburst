package org.cloudburstmc.api.block;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.math.vector.Vector4i;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;

public interface Block extends BlockSnapshot {

    BlockSnapshot snapshot();

    Block refresh();

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
        return getSide(Direction.UP, 1);
    }

    default Block up(int step) {
        return getSide(Direction.UP, step);
    }

    default Block down() {
        return getSide(Direction.DOWN, 1);
    }

    default Block down(int step) {
        return getSide(Direction.DOWN, step);
    }

    default Block north() {
        return getSide(Direction.NORTH, 1);
    }

    default Block north(int step) {
        return getSide(Direction.NORTH, step);
    }

    default Block east() {
        return getSide(Direction.EAST, 1);
    }

    default Block east(int step) {
        return getSide(Direction.EAST, step);
    }

    default Block south() {
        return getSide(Direction.SOUTH, 1);
    }

    default Block south(int step) {
        return getSide(Direction.SOUTH, step);
    }

    default Block west() {
        return getSide(Direction.WEST, 1);
    }

    default Block west(int step) {
        return getSide(Direction.WEST, step);
    }

    default Block getSide(Direction face) {
        return getSide(face, 1);
    }

    default BlockState upState() {
        return getSideState(Direction.UP, 1);
    }

    default BlockState upState(int step) {
        return getSideState(Direction.UP, step);
    }

    default BlockState downState() {
        return getSideState(Direction.DOWN, 1);
    }

    default BlockState downState(int step) {
        return getSideState(Direction.DOWN, step);
    }

    default BlockState northState() {
        return getSideState(Direction.NORTH, 1);
    }

    default BlockState northState(int step) {
        return getSideState(Direction.NORTH, step);
    }

    default BlockState eastState() {
        return getSideState(Direction.EAST, 1);
    }

    default BlockState eastState(int step) {
        return getSideState(Direction.EAST, step);
    }

    default BlockState southState() {
        return getSideState(Direction.SOUTH, 1);
    }

    default BlockState southState(int step) {
        return getSideState(Direction.SOUTH, step);
    }

    default BlockState westState() {
        return getSideState(Direction.WEST, 1);
    }

    default BlockState westState(int step) {
        return getSideState(Direction.WEST, step);
    }

    default BlockState getSideState(Direction face) {
        return getSideState(face, 1);
    }

    Block getSide(Direction face, int step);

    default BlockState getSideState(Direction face, int step) {
        return getSideState(face, step, 0);
    }

    BlockState getSideState(Direction face, int step, int layer);

    default Block getRelative(Vector3i pos) {
        return getRelative(pos.getX(), pos.getY(), pos.getZ());
    }

    Block getRelative(int x, int y, int z);

    default BlockState getRelativeState(int x, int y, int z) {
        return getRelativeState(x, y, z, 0);
    }

    default BlockState getRelativeState(Vector3i pos) {
        return getRelativeState(pos.getX(), pos.getY(), pos.getZ(), 0);
    }

    default BlockState getRelativeState(Vector4i pos) {
        return getRelativeState(pos.getX(), pos.getY(), pos.getZ(), pos.getW());
    }

    default BlockState getRelativeState(Vector3i pos, int layer) {
        return getRelativeState(pos.getX(), pos.getY(), pos.getZ(), layer);
    }

    BlockState getRelativeState(int x, int y, int z, int layer);

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
