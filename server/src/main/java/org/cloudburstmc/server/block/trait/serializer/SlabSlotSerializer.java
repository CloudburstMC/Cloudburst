package org.cloudburstmc.server.block.trait.serializer;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
public class SlabSlotSerializer implements TraitSerializer<SlabSlot> {

    @Override
    public Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<SlabSlot> trait, SlabSlot slot) {
        return switch (slot) {
            case BOTTOM -> "bottom";
            case TOP -> "top";
        };
    }

    @Override
    public String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<SlabSlot> blockTrait) {
        return BedrockStateTags.TAG_MINECRAFT_VERTICAL_HALF;
    }
}
