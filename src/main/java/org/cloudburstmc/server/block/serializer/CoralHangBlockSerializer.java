package org.cloudburstmc.server.block.serializer;

import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_CORAL_HANG_TYPE_BIT;

public class CoralHangBlockSerializer implements BlockSerializer {

    private static final String[] BEDROCK_NAMES = {
            "minecraft:coral_fan_hang",
            "minecraft:coral_fan_hang2",
            "minecraft:coral_fan_hang3"
    };

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        DefaultBlockSerializer.INSTANCE.serialize(builder, blockType, traits);

        NbtMap states = (NbtMap) builder.get(TAG_STATES);

        DyeColor color = (DyeColor) traits.get(BlockTraits.CORAL_HANG_COLOR);
        int index = BlockTraits.CORAL_HANG_COLOR.getIndex(color);
        boolean bit = (index & 0b1) != 0;

        String name = BEDROCK_NAMES[index >> 1];

        builder.putCompound(TAG_STATES, states.toBuilder()
                .putBoolean(TAG_CORAL_HANG_TYPE_BIT, bit)
                .build());
        builder.putString(TAG_NAME, name);
    }
}
