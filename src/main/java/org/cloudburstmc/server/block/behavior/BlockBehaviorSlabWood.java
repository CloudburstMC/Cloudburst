package org.cloudburstmc.server.block.behavior;

import lombok.var;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.data.SlabSlot;

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
            state = state.resetTrait(BlockTraits.SLAB_SLOT);
        }

        return ItemStack.get(state);
    }
}
