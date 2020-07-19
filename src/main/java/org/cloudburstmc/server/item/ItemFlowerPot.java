package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Snake1999 on 2016/2/4.
 * Package cn.nukkit.item in project Nukkit.
 */
public class ItemFlowerPot extends Item {

    public ItemFlowerPot(Identifier id) {
        super(id);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(BlockTypes.FLOWER_POT);
    }
}
