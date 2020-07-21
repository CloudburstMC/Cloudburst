package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

/**
 * Created by Pub4Game on 26.12.2015.
 */
public class BlockBehaviorWallSign extends BlockBehaviorSignPost {

    public BlockBehaviorWallSign(Identifier id, Identifier signStandingId, Identifier signItemId) {
        super(id, signStandingId, id, signItemId);
    }

    @Override
    public int onUpdate(Block block, int type) {
        int[] faces = {
                3,
                2,
                5,
                4,
        };
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.getMeta() >= 2 && this.getMeta() <= 5) {
                if (this.getSide(BlockFace.fromIndex(faces[this.getMeta() - 2])).getId() == AIR) {
                    this.getLevel().useBreakOn(this.getPosition());
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    public static BlockFactory factory(Identifier signStandingId, Identifier signItemId) {
        return signWallId -> new BlockBehaviorWallSign(signWallId, signStandingId, signItemId);
    }
}
