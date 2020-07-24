package org.cloudburstmc.server.block.trait;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;

@FunctionalInterface
public interface BlockTraitSerializer<T extends Comparable<T>> {

    Comparable<?> serialize(NbtMapBuilder builder, BlockState state, T value);
}
