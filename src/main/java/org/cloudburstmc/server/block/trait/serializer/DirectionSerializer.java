package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.val;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockIds;
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

        val type = state.getType();

        if (type == BlockIds.CORAL_FAN_HANG || type == BlockIds.CORAL_FAN_HANG2 || type == BlockIds.CORAL_FAN_HANG3) {
            return "coral_direction";
        }

        return null;
    }
}
