package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;

public class BlockBehaviorPumpkinLit extends BlockBehaviorPumpkin {

    public BlockBehaviorPumpkinLit() {
        super(BlockIds.LIT_PUMPKIN);
    }

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

}
