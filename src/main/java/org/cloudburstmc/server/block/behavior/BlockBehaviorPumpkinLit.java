package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;

public class BlockBehaviorPumpkinLit extends BlockBehaviorPumpkin {

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

}
