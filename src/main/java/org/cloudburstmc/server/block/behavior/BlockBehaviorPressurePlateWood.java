package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPressurePlateWood extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorPressurePlateWood() {
        this.onPitch = 0.8f;
        this.offPitch = 0.7f;
    }


    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                toItem(block)
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength(Block block) {
        AxisAlignedBB bb = getCollisionBoxes(block.getPosition(), block.getState());

        for (Entity entity : block.getLevel().getCollidingEntities(bb)) {
            if (entity.canTriggerPressurePlate()) {
                return 15;
            }
        }

        return 0;
    }
}