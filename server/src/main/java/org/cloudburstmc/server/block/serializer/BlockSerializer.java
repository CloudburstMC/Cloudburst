package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BooleanBlockTrait;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.block.trait.IntegerBlockTrait;

@SuppressWarnings("ConstantConditions")
public interface BlockSerializer {

    void serialize(NbtMapBuilder builder, BlockState state);

    default <T extends Comparable<T>> void serialize(NbtMapBuilder builder, BlockState state, BlockTrait<T> trait) {
        if (trait instanceof BooleanBlockTrait) {
            serialize(builder, state, (BooleanBlockTrait) trait);
            return;
        }

        if (trait instanceof IntegerBlockTrait) {
            serialize(builder, state, (IntegerBlockTrait) trait);
            return;
        }

        if (trait instanceof EnumBlockTrait<?>) {
            serialize(builder, state, (EnumBlockTrait<?>) trait);
            return;
        }

        throw new IllegalArgumentException(String.format("Could not serialize BlockTrait %s", trait.getClass().getName()));
    }

    default void serialize(NbtMapBuilder builder, BlockState state, BooleanBlockTrait trait) {
        builder.putBoolean(trait.getName(), state.getTrait(trait));
    }

    default void serialize(NbtMapBuilder builder, BlockState state, IntegerBlockTrait trait) {
        builder.putInt(trait.getName(), state.getTrait(trait));
    }

    default <E extends Enum<E>> void serialize(NbtMapBuilder builder, BlockState state, EnumBlockTrait<E> trait) {
        builder.putString(trait.getName(), state.getTrait(trait).name().toLowerCase());
    }
}
