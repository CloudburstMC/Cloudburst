package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
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
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.STONE_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength() {
        AxisAlignedBB bb = getCollisionBoxes();

        for (Entity entity : this.level.getCollidingEntities(bb)) {
            if (entity instanceof EntityLiving && entity.canTriggerPressurePlate()) {
                return 15;
            }
        }

        return 0;
    }
}
