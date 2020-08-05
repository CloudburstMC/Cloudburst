package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.math.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class DirectionSerializer implements TraitSerializer<Direction> {

    static {
        DirectionHelper.init();
    }

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, Direction direction) {
        return DirectionHelper.serialize(builder, state);
    }

    @Override
    public String getName(BlockState state, BlockTrait<?> blockTrait) {
        if (state.inCategory(BlockCategory.STAIRS)) {
            return "weirdo_direction";
        }

        return null;
    }
}
