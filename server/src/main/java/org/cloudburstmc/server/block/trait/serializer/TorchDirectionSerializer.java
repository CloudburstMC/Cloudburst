package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class TorchDirectionSerializer implements TraitSerializer<Direction> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, Direction direction) {
        if (direction == Direction.UP) {
            return "top";
        }

        if (direction == Direction.DOWN) {
            return "unknown";
        }

        return direction.name().toLowerCase();
    }
}
