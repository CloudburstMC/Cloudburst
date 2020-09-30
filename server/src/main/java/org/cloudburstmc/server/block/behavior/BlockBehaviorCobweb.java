package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorCobweb extends FloodableBlockBehavior {



    @Override
    public float getResistance() {
        return 20;
    }

    @Override
    public ToolType getToolType(BlockState state) {
        return ItemToolBehavior.TYPE_SWORD;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        entity.resetFallDistance();
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.isShears() || hand.isSword()) {
            return new ItemStack[]{
                    ItemStack.get(ItemTypes.STRING)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.CLOTH_BLOCK_COLOR;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
