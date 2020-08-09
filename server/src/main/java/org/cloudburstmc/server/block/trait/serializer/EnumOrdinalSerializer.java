package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EnumOrdinalSerializer<E extends Enum<E>> implements TraitSerializer<E> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, E e) {
        return e.ordinal();
    }
}
