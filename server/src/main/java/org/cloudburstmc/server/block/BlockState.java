package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface BlockState {

    BlockState AIR = BlockPalette.INSTANCE.air;

    @Nonnull
    Identifier getType();

    @Nullable
    <T extends Comparable<T>> T getTrait(BlockTrait<T> trait);

    @Nonnull
    <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait);

    @Nonnull
    ImmutableMap<BlockTrait<?>, Comparable<?>> getTraits();

    @Nonnull
    <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value);

    @Nonnull
    BlockState withTrait(IntegerBlockTrait trait, int value);

    @Nonnull
    BlockState withTrait(BooleanBlockTrait trait, boolean value);

    static BlockState get(@Nonnull Identifier blockType) {
        return BlockPalette.INSTANCE.getDefaultState(blockType);
    }
}
