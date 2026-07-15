package org.cloudburstmc.api.block;

import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.math.vector.Vector4i;

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

    ComponentMap getComponents();

    int getBrightness();

    boolean isWaterlogged();

    /**
     * Gets the level-aware selection and interaction outline of this block.
     *
     * @return the outline shape
     */
    VoxelShape getOutlineShape();

    /**
     * Gets the geometry this block uses to support neighboring blocks.
     *
     * @return the block support shape
     */
    VoxelShape getBlockSupportShape();

    /**
     * Checks whether one face provides the requested kind of block support.
     *
     * @param face the face to check
     * @param supportType the required support type
     * @return {@code true} if the face is sturdy for that support type
     */
    boolean isFaceSturdy(Direction face, SupportType supportType);

    default Block up() {
        return getSide(Direction.UP, 1);
    }

    default Block getSide(Direction face) {
        return getSide(face, 1);
    }

    Block getSide(Direction face, int step);

    default BlockState getSideState(Direction face) {
        return getSideState(face, 1);
    }

    default BlockState getSideState(Direction face, int step) {
        return getSideState(face, step, 0);
    }

    BlockState getSideState(Direction face, int step, int layer);

    default Block getRelative(Vector3i position) {
        return getRelative(position.getX(), position.getY(), position.getZ());
    }

    Block getRelative(int x, int y, int z);

    default BlockState getRelativeState(int x, int y, int z) {
        return getRelativeState(x, y, z, 0);
    }

    default BlockState getRelativeState(Vector3i position) {
        return getRelativeState(position.getX(), position.getY(), position.getZ(), 0);
    }

    default BlockState getRelativeState(Vector4i position) {
        return getRelativeState(position.getX(), position.getY(), position.getZ(), position.getW());
    }

    default BlockState getRelativeState(Vector3i position, int layer) {
        return getRelativeState(position.getX(), position.getY(), position.getZ(), layer);
    }

    BlockState getRelativeState(int x, int y, int z, int layer);

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
