package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SlabSlotSerializer implements TraitSerializer<SlabSlot> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, SlabSlot slot) {
        return switch (slot) {
            case BOTTOM -> "bottom";
            case TOP -> "top";
            case FULL -> "bottom";
        };
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
        return BedrockStateTags.TAG_MINECRAFT_VERTICAL_HALF;
    }
}
