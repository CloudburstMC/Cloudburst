package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPressurePlateStone extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorPressurePlateStone() {
        this.onPitch = 0.6f;
        this.offPitch = 0.5f;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.STONE_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength(Block block) {
        AxisAlignedBB bb = getCollisionBoxes(block.getPosition(), block.getState());

        for (Entity entity : block.getLevel().getCollidingEntities(bb)) {
            if (entity instanceof EntityLiving && entity.canTriggerPressurePlate()) {
                return 15;
            }
        }

        return 0;
    }
}
