package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorShulkerBox extends BlockBehaviorUndyedShulkerBox {

    @Override
    public BlockColor getColor(Block block) {
        return this.getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        val color = block.getState().getTrait(BlockTraits.COLOR);

        if (color == null) {
            return DyeColor.WHITE;
        }

        return color;
    }
}
