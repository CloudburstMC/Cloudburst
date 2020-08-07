package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.data.DyeColor;

import static org.cloudburstmc.server.block.serializer.util.BedrockStateTags.TAG_CORAL_HANG_TYPE_BIT;

public class CoralHangBlockSerializer extends DefaultBlockSerializer {

    private static final String[] BEDROCK_NAMES = {
            "minecraft:coral_fan_hang",
            "minecraft:coral_fan_hang2",
            "minecraft:coral_fan_hang3"
    };

    @Override
    public void serialize(NbtMapBuilder builder, BlockState state) {
        super.serialize(builder, state);

        NbtMap states = (NbtMap) builder.get(TAG_STATES);

        DyeColor color = state.ensureTrait(BlockTraits.CORAL_HANG_COLOR);
        int index = BlockTraits.CORAL_HANG_COLOR.getIndex(color);
        boolean bit = (index & 0b1) != 0;

        String name = BEDROCK_NAMES[index >> 1];

        builder.putCompound(TAG_STATES, states.toBuilder()
                .putBoolean(TAG_CORAL_HANG_TYPE_BIT, bit)
                .build());
        builder.putString(TAG_NAME, name);
    }
}
