package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.behavior.Item;

import static org.cloudburstmc.server.block.BlockIds.DAYLIGHT_DETECTOR;

public class BlockBehaviorDaylightDetectorInverted extends BlockBehaviorDaylightDetector {

    @Override
    public Item toItem(Block block) {
        return Item.get(DAYLIGHT_DETECTOR);
    }

    protected boolean invertDetect() {
        return true;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
