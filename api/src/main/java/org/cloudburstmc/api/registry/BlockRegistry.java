package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.api.util.component.ComponentMap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry for block types, block states, block components, and block tags.
 */
public interface BlockRegistry extends ComponentRegistry<BlockType>, KeyedRegistry<BlockType> {

    /**
     * Returns the default state for a block type.
     *
     * @param type block type
     * @return default state for the type
     */
    BlockState getBlock(BlockType type);

    /**
     * Returns the placed block state represented by an item stack.
     *
     * @param item stack to resolve
     * @return placed block state, or {@code null} if the item does not place a block
     */
    @Nullable
    BlockState getBlock(ItemStack item);

    /**
     * Returns the default state for a block identifier.
     *
     * @param id block identifier
     * @return default state, or {@code null} if no block is registered for the identifier
     */
    @Nullable
    BlockState getBlock(Identifier id);

    /**
     * Returns every registered block state.
     *
     * @return immutable list of block states
     */
    List<BlockState> getBlockStates();

    @Override
    default Optional<BlockType> get(Identifier id) {
        BlockState state = this.getBlock(id);
        return state == null ? Optional.empty() : Optional.of(state.getType());
    }

    @Override
    default Identifier getId(BlockType value) {
        return value.getId();
    }

    @Override
    default Collection<BlockType> values() {
        return this.getBlockStates().stream()
                .map(BlockState::getType)
                .distinct()
                .toList();
    }

    @Override
    @Nullable
    ComponentMap getComponents(BlockType type);

    /**
     * Returns the collision/support shape used when this block supports another block.
     *
     * @param state block state
     * @return block support shape
     */
    VoxelShape getBlockSupportShape(BlockState state);

    /**
     * Returns whether a face can provide the requested support type.
     *
     * @param state       block state
     * @param face        queried face
     * @param supportType requested support behavior
     * @return whether the face is sturdy for the requested support type
     */
    boolean isFaceSturdy(BlockState state, Direction face, SupportType supportType);

    /**
     * Returns the block tag identified by a key.
     *
     * @param key tag key to resolve
     * @return matching read-only tag
     * @throws IllegalArgumentException if the key is unknown
     */
    BlockTag getTag(BlockTagKey key);

    /**
     * Returns a builder for configuring a registered block type during initialization.
     *
     * @param type registered block type
     * @return component builder for the type
     * @throws RegistryException if the type is unknown or registration has closed
     */
    ComponentBuilder configure(BlockType type) throws RegistryException;

}
