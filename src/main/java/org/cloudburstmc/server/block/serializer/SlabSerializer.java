package org.cloudburstmc.server.block.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.data.SlabSlot;

import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SlabSerializer implements BlockSerializer {

    public static final SlabSerializer INSTANCE = new SlabSerializer();

    private void serialize0(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits, boolean flag) {
        DefaultBlockSerializer.INSTANCE.serialize(builder, blockType, traits);
        val statesBuilder = ((NbtMap) builder.get("states")).toBuilder();

        switch ((SlabSlot) traits.get(BlockTraits.SLAB_SLOT)) {
            case TOP:
                statesBuilder.remove("slab_slot");
                statesBuilder.putBoolean(BedrockStateTags.TAG_TOP_SLOT_BIT, true);
                break;
            case BOTTOM:
                statesBuilder.remove("slab_slot");
                statesBuilder.putBoolean(BedrockStateTags.TAG_TOP_SLOT_BIT, false);
                break;
            default:
                statesBuilder.putBoolean(BedrockStateTags.TAG_TOP_SLOT_BIT, flag);
                break;
        }

        builder.putCompound("states", statesBuilder.build());

        if (blockType == BlockTypes.STONE_SLAB) {
            MultiBlockSerializers.STONE_SLAB.serialize(builder, blockType, traits);
        } else {
            MultiBlockSerializers.WOODEN_SLAB.serialize(builder, blockType, traits);
        }
    }

    @Override
    public void serialize(NbtMapBuilder builder, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        serialize0(builder, blockType, traits, false);
    }

    @Override
    public void serialize(List<NbtMapBuilder> tags, BlockType blockType, Map<BlockTrait<?>, Comparable<?>> traits) {
        val slabSlot = (SlabSlot) traits.get(BlockTraits.SLAB_SLOT);

        if (slabSlot == SlabSlot.FULL) {
            NbtMapBuilder builder = NbtMap.builder();
            tags.add(builder);

            serialize0(builder, blockType, traits, true);
        }

        NbtMapBuilder builder = NbtMap.builder();
        tags.add(builder);

        serialize(builder, blockType, traits);
    }
}
