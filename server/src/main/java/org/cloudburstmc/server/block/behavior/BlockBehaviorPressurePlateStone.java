package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.entity.EntityLiving;

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
