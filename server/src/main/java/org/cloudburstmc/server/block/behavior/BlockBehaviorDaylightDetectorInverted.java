package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import static org.cloudburstmc.api.block.BlockTypes.DAYLIGHT_DETECTOR;

public class BlockBehaviorDaylightDetectorInverted extends BlockBehaviorDaylightDetector {

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(DAYLIGHT_DETECTOR);
    }

    protected boolean invertDetect() {
        return true;
    }


}
