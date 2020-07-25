package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created by PetteriM1
 */
public class BlockBehaviorShulkerBox extends BlockBehaviorUndyedShulkerBox {

    public BlockBehaviorShulkerBox(Identifier id) {
        super(id);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return this.getDyeColor().getColor();
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByWoolData(this.getMeta());
    }
}
