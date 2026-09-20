package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * A level position together with the block states captured at that position.
 *
 * <p>A block does not update its captured states when the level changes. Use
 * {@link #refresh()} to obtain a block containing the current states.
 */
public interface Block extends BlockSnapshot {
    /**
     * Returns the level containing this block.
     *
     * @return the level
     */
    Level getLevel();

    /**
     * Returns the chunk containing this block, loading it when necessary.
     *
     * @return the containing chunk
     */
    Chunk getChunk();

    /**
     * Returns this block's level position.
     *
     * @return the block position
     */
    Vector3i getPosition();

    /**
     * Returns this block's X coordinate.
     *
     * @return the X coordinate
     */
    default int getX() {
        return this.getPosition().getX();
    }

    /**
     * Returns this block's Y coordinate.
     *
     * @return the Y coordinate
     */
    default int getY() {
        return this.getPosition().getY();
    }

    /**
     * Returns this block's Z coordinate.
     *
     * @return the Z coordinate
     */
    default int getZ() {
        return this.getPosition().getZ();
    }

    /**
     * Returns a detached snapshot of the captured states.
     *
     * @return the block-state snapshot
     */
    BlockSnapshot snapshot();

    /**
     * Returns a block containing the states currently stored at this position.
     *
     * @return the refreshed block
     */
    Block refresh();

    /**
     * Returns the effective combined light at this position.
     *
     * @return the light level from {@code 0} to {@code 15}
     */
    int getLightLevel();

    /**
     * Returns the sky light at this position.
     *
     * @return the sky light level from {@code 0} to {@code 15}
     */
    int getSkyLight();

    /**
     * Returns the propagated light from block sources at this position.
     *
     * @return the block light level from {@code 0} to {@code 15}
     */
    int getBlockLight();

    /**
     * Returns the behavioral components of the captured primary block type.
     *
     * @return the block components
     */
    ComponentMap getComponents();

    /**
     * Returns a behavioral component of the captured primary block type.
     *
     * @param type the component type
     * @param <H>  the component value type
     * @return the component, or {@code null} when it is not present
     */
    default <H> @Nullable H getComponent(ComponentType<H> type) {
        return this.getComponents().get(type);
    }

    /**
     * Returns a required behavioral component of the captured primary block type.
     *
     * @param type the component type
     * @param <H>  the component value type
     * @return the component
     * @throws IllegalStateException when the component is not present
     */
    default <H> H requireComponent(ComponentType<H> type) {
        return this.getComponents().require(type);
    }

    /**
     * Returns the collision shape at this position.
     *
     * @return the collision shape
     */
    VoxelShape getCollisionShape();

    /**
     * Returns the selection and interaction outline at this position.
     *
     * @return the outline shape
     */
    VoxelShape getOutlineShape();

    /**
     * Returns the geometry used to support neighboring blocks.
     *
     * @return the block support shape
     */
    VoxelShape getBlockSupportShape();

    /**
     * Checks whether a face provides the requested kind of block support.
     *
     * @param face        the face to check
     * @param supportType the required support type
     * @return {@code true} when the face provides the requested support
     */
    boolean isFaceSturdy(Direction face, SupportType supportType);

    /**
     * Returns the block directly above this block.
     *
     * @return the block above
     */
    default Block up() {
        return this.getSide(Direction.UP);
    }

    /**
     * Returns the adjacent block in a direction.
     *
     * @param face the direction from this block
     * @return the adjacent block
     */
    default Block getSide(Direction face) {
        return this.getSide(face, 1);
    }

    /**
     * Returns a block a number of steps in a direction.
     *
     * @param face the direction from this block
     * @param step the number of blocks to move
     * @return the relative block
     */
    Block getSide(Direction face, int step);

    /**
     * Returns the primary state of the adjacent block in a direction.
     *
     * @param face the direction from this block
     * @return the adjacent primary state
     */
    default BlockState getSideState(Direction face) {
        return this.getSideState(face, 1);
    }

    /**
     * Returns the primary state a number of steps in a direction.
     *
     * @param face the direction from this block
     * @param step the number of blocks to move
     * @return the relative primary state
     */
    default BlockState getSideState(Direction face, int step) {
        return this.getSideState(face, step, BlockLayer.PRIMARY);
    }

    /**
     * Returns a state a number of steps in a direction.
     *
     * @param face  the direction from this block
     * @param step  the number of blocks to move
     * @param layer the block layer
     * @return the relative block state
     */
    BlockState getSideState(Direction face, int step, BlockLayer layer);

    /**
     * Returns a block at an offset from this block.
     *
     * @param offset the relative offset
     * @return the relative block
     */
    default Block getRelative(Vector3i offset) {
        return this.getRelative(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Returns a block at an offset from this block.
     *
     * @param x the relative X offset
     * @param y the relative Y offset
     * @param z the relative Z offset
     * @return the relative block
     */
    Block getRelative(int x, int y, int z);

    /**
     * Returns the primary state at an offset from this block.
     *
     * @param x the relative X offset
     * @param y the relative Y offset
     * @param z the relative Z offset
     * @return the relative primary state
     */
    default BlockState getRelativeState(int x, int y, int z) {
        return this.getRelativeState(x, y, z, BlockLayer.PRIMARY);
    }

    /**
     * Returns the primary state at an offset from this block.
     *
     * @param offset the relative offset
     * @return the relative primary state
     */
    default BlockState getRelativeState(Vector3i offset) {
        return this.getRelativeState(offset, BlockLayer.PRIMARY);
    }

    /**
     * Returns a state at an offset from this block.
     *
     * @param offset the relative offset
     * @param layer  the block layer
     * @return the relative block state
     */
    default BlockState getRelativeState(Vector3i offset, BlockLayer layer) {
        return this.getRelativeState(offset.getX(), offset.getY(), offset.getZ(), layer);
    }

    /**
     * Returns a state at an offset from this block.
     *
     * @param x     the relative X offset
     * @param y     the relative Y offset
     * @param z     the relative Z offset
     * @param layer the block layer
     * @return the relative block state
     */
    BlockState getRelativeState(int x, int y, int z, BlockLayer layer);

    /**
     * Replaces the primary state and performs normal block updates.
     *
     * @param state the replacement state
     */
    default void set(BlockState state) {
        this.set(state, BlockLayer.PRIMARY, false, true);
    }

    /**
     * Replaces the primary state and performs normal block updates.
     *
     * @param state  the replacement state
     * @param direct whether to send the change immediately instead of batching it
     */
    default void set(BlockState state, boolean direct) {
        this.set(state, BlockLayer.PRIMARY, direct, true);
    }

    /**
     * Replaces the primary state.
     *
     * @param state  the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, block entities, liquids, and neighboring blocks
     */
    default void set(BlockState state, boolean direct, boolean update) {
        this.set(state, BlockLayer.PRIMARY, direct, update);
    }

    /**
     * Replaces the secondary state and performs normal block updates.
     *
     * @param state the replacement state
     */
    default void setSecondaryState(BlockState state) {
        this.set(state, BlockLayer.SECONDARY, false, true);
    }

    /**
     * Replaces the secondary state and performs normal block updates.
     *
     * @param state  the replacement state
     * @param direct whether to send the change immediately instead of batching it
     */
    default void setSecondaryState(BlockState state, boolean direct) {
        this.set(state, BlockLayer.SECONDARY, direct, true);
    }

    /**
     * Replaces the secondary state.
     *
     * @param state  the replacement state
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, block entities, liquids, and neighboring blocks
     */
    default void setSecondaryState(BlockState state, boolean direct, boolean update) {
        this.set(state, BlockLayer.SECONDARY, direct, update);
    }

    /**
     * Replaces a state at this block position.
     *
     * <p>The captured states of this block are unchanged. Use {@link #refresh()}
     * to obtain the states stored after this operation.
     *
     * @param state  the replacement state
     * @param layer  the block layer to replace
     * @param direct whether to send the change immediately instead of batching it
     * @param update whether to process lighting, block entities, liquids, and neighboring blocks
     */
    void set(BlockState state, BlockLayer layer, boolean direct, boolean update);
}
