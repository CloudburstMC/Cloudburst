package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class BlockBehaviorSlabWood extends BlockBehaviorSlab {

    static final BlockColor[] COLORS = new BlockColor[]{
            BlockColor.WOOD_BLOCK_COLOR,
            BlockColor.SPRUCE_BLOCK_COLOR,
            BlockColor.SAND_BLOCK_COLOR,
            BlockColor.DIRT_BLOCK_COLOR,
            BlockColor.ORANGE_BLOCK_COLOR,
            BlockColor.BROWN_BLOCK_COLOR
    };

    @Override
    public ItemStack toItem(Block block) {
        var state = block.getState();
        if (block.getState().ensureTrait(BlockTraits.SLAB_SLOT) == SlabSlot.TOP) {
            state = state.withTrait(BlockTraits.SLAB_SLOT, BlockTraits.SLAB_SLOT.getDefaultValue());
        }

        return CloudItemRegistry.get().getItem(state);
    }
}
