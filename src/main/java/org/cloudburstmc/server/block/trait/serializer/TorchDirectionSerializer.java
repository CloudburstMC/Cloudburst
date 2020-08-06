package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.math.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TorchDirectionSerializer implements TraitSerializer<Direction> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, Direction direction) {
        return direction.name().toLowerCase();
    }
}
