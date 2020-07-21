package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by PetteriM1
 */
public class BlockBehaviorWallBanner extends BlockBehaviorBanner {

    public BlockBehaviorWallBanner(Identifier id) {
        super(id);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.getMeta() >= BlockFace.NORTH.getIndex() && this.getMeta() <= BlockFace.EAST.getIndex()) {
                if (this.getSide(BlockFace.fromIndex(this.getMeta()).getOpposite()).getId() == BlockTypes.AIR) {
                    this.getLevel().useBreakOn(this.getPosition());
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }
}
