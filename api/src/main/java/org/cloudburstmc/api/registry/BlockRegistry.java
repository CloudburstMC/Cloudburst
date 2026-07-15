package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.component.ComponentMap;

public interface BlockRegistry extends ComponentRegistry<BlockType> {

    boolean isBlock(Identifier id);

    int getRuntimeId(BlockState state);

    int getRuntimeId(Identifier id, int meta);

    BlockState getBlock(BlockType type);

    BlockState getBlock(ItemStack item);

    BlockState getBlock(Identifier id);

    BlockState getBlock(Identifier id, int meta);

    BlockState getBlock(int runtimeId);

    ImmutableList<BlockState> getBlockStates();

    @Override
    ComponentMap getComponents(BlockType type);

    VoxelShape getBlockSupportShape(BlockState state);

    boolean isFaceSturdy(BlockState state, Direction face, SupportType supportType);

    /**
     * Returns the block tag identified by a key.
     *
     * @param key tag key to resolve
     * @return matching read-only tag
     * @throws IllegalArgumentException if the key is unknown
     */
    BlockTag getTag(BlockTagKey key);

    ComponentMap register(BlockType type) throws RegistryException;

}
