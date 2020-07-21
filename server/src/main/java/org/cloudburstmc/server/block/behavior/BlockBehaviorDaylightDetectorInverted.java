package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;

import static org.cloudburstmc.server.block.BlockTypes.DAYLIGHT_DETECTOR;

public class BlockBehaviorDaylightDetectorInverted extends BlockBehaviorDaylightDetector {

    @Override
    public Item toItem(BlockState state) {
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
