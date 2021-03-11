package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.api.util.data.DyeColor;

public class BlockBehaviorShulkerBox extends BlockBehaviorUndyedShulkerBox {

    @Override
    public BlockColor getColor(Block block) {
        return this.getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        val color = block.getState().ensureTrait(BlockTraits.COLOR);

        if (color == null) {
            return DyeColor.WHITE;
        }

        return color;
    }
}
