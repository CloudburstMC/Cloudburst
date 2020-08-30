package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
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
        return BlockState.get(BlockIds.FLOWER_POT);
    }
}
