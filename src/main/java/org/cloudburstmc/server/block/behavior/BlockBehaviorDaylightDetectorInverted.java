package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.DAYLIGHT_DETECTOR;

/**
 * Created on 2015/11/22 by CreeperFace.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorDaylightDetectorInverted extends BlockBehaviorDaylightDetector {

    public BlockBehaviorDaylightDetectorInverted(Identifier id) {
        super(id);
    }

    @Override
    public Item toItem() {
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
