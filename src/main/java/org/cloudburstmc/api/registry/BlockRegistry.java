package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public interface BlockRegistry extends Registry {

    void register(BlockType type, BlockBehavior behavior) throws RegistryException;

    BlockBehavior getBehavior(BlockType type);

    void overwriteBehavior(BlockType type, BlockBehavior behavior);

    boolean isBlock(Identifier id);

    int getRuntimeId(BlockState state);

    int getRundtimeId(Identifier id, int meta);

    BlockState getBlock(BlockType type);

    BlockState getBlock(ItemStack item);

    BlockState getBlock(Identifier id);

    BlockState getBlock(Identifier id, int meta);

    BlockState getBlock(int runtimeId);

    ImmutableList<BlockState> getBlockStates();

}
