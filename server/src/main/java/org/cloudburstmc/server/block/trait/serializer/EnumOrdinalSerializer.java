package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class EnumOrdinalSerializer<E extends Enum<E>> implements TraitSerializer<E> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<E> trait, E value) {
        return value.ordinal();
    }
}
