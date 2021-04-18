package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class BlockBehaviorLog2 extends BlockBehaviorLog {

    public static void upgradeLegacyBlock(int[] blockState) {
        if ((blockState[1] & 0b1100) == 0b1100) { // old full bark texture
            blockState[0] = CloudBlockRegistry.get().getLegacyId(BlockIds.WOOD);
            blockState[1] = (blockState[1] & 0x03) + 4; // gets only the log type and set pillar to y
        }
    }
}
