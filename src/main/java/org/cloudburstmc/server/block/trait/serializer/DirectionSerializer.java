package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.BlockCategories;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class DirectionSerializer implements TraitSerializer<Direction> {

    static {
        DirectionHelper.init();
    }

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, Direction direction) {
        return DirectionHelper.serialize(builder, type, traits);
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        if (BlockCategories.inCategory(type, BlockCategory.STAIRS)) {
            return "weirdo_direction";
        }

        if (type == BlockTypes.CORAL_FAN_HANG) {
            return "coral_direction";
        }

        return null;
    }
}
