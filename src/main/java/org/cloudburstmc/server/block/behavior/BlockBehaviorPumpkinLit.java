package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTypes;

public class BlockBehaviorPumpkinLit extends BlockBehaviorPumpkin {

    public BlockBehaviorPumpkinLit() {
        super(BlockTypes.LIT_PUMPKIN);
    }

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

}
