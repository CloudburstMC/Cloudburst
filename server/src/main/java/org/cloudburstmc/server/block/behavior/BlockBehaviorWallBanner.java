package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;

public class BlockBehaviorWallBanner extends BlockBehaviorBanner {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.getMeta() >= Direction.NORTH.getIndex() && this.getMeta() <= Direction.EAST.getIndex()) {
                if (this.getSide(Direction.fromIndex(this.getMeta()).getOpposite()).getId() == BlockTypes.AIR) {
                    this.getLevel().useBreakOn(this.getPosition());
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }
}
