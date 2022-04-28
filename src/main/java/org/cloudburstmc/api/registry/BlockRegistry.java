package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;

public interface BlockRegistry extends BehaviorRegistry<BlockType> {

    BehaviorCollection register(BlockType type) throws RegistryException;

    BehaviorCollection getBehaviors(BlockType type);

    default <T> T getBehavior(BlockType type, BehaviorKey<?, T> key) {
        return getBehaviors(type).get(key);
    }

    boolean isBlock(Identifier id);

    int getRuntimeId(BlockState state);

    int getRuntimeId(Identifier id, int meta);

    BlockState getBlock(BlockType type);

    BlockState getBlock(ItemStack item);

    BlockState getBlock(Identifier id);

    BlockState getBlock(Identifier id, int meta);

    BlockState getBlock(int runtimeId);

    ImmutableList<BlockState> getBlockStates();

}
