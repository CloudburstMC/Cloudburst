package org.cloudburstmc.server.block;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
@JsonDeserialize(using = BlockStateDeserializer.class)
public interface BlockState {

    @Nonnull
    Identifier getId();

    @Nonnull
    BlockType getType();

    @Nullable
    <T extends Comparable<T>> T getTrait(BlockTrait<T> trait);

    @Nonnull
    <T extends Comparable<T>> T ensureTrait(BlockTrait<T> trait);

    @Nonnull
    ImmutableMap<BlockTrait<?>, Comparable<?>> getTraits();

    @Nonnull
    <T extends Comparable<T>> BlockState withTrait(BlockTrait<T> trait, T value);

    @Nonnull
    default <T extends Comparable<T>> BlockState copyTrait(BlockTrait<T> trait, BlockState from) {
        return withTrait(trait, from.ensureTrait(trait));
    }

    @Nonnull
    BlockState copyTraits(BlockState from);

    @Nonnull
    BlockState withTrait(IntegerBlockTrait trait, int value);

    @Nonnull
    default BlockState incrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.min(trait.getRange().getEnd(), ensureTrait(trait) + 1));
    }

    @Nonnull
    default BlockState decrementTrait(IntegerBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, Math.max(trait.getRange().getStart(), ensureTrait(trait) - 1));
    }

    @Nonnull
    BlockState withTrait(BooleanBlockTrait trait, boolean value);

    @Nonnull
    default BlockState toggleTrait(BooleanBlockTrait trait) {
        checkNotNull(trait, "trait");
        return withTrait(trait, !ensureTrait(trait));
    }

    default <T extends Comparable<T>> BlockState resetTrait(BlockTrait<T> trait) {
        return this.withTrait(trait, trait.getDefaultValue());
    }

    BlockBehavior getBehavior();

    @Nonnull
    BlockState defaultState();

    default boolean inCategory(BlockCategory category) {
        return BlockCategories.inCategory(this.getType(), category);
    }

    static BlockState get(@Nonnull Identifier blockType) {
        return BlockPalette.INSTANCE.getDefaultState(blockType);
    }
}
