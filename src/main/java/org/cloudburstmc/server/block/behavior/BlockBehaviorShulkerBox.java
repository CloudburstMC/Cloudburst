package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorShulkerBox extends BlockBehaviorUndyedShulkerBox {

    @Override
    public BlockColor getColor(Block block) {
        return this.getDyeColor().getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(this.getMeta());
    }
}
