package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSoulSand extends BlockBehaviorSolid {


//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 1 - 0.125f;
//    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        entity.setMotion(entity.getMotion().mul(0.4, 1, 0.4));
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.BROWN_BLOCK_COLOR;
    }

}
