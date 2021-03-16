package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.data.BlockColor;

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
