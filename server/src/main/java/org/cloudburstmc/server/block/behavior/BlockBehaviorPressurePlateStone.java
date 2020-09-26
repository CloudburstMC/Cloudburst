package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPressurePlateStone extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorPressurePlateStone() {
        this.onPitch = 0.6f;
        this.offPitch = 0.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.STONE_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength(Block block) {
        AxisAlignedBB bb = getCollisionBoxes(block);

        for (Entity entity : block.getLevel().getCollidingEntities(bb)) {
            if (entity instanceof EntityLiving && entity.canTriggerPressurePlate()) {
                return 15;
            }
        }

        return 0;
    }
}
