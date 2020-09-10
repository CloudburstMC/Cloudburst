package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.TerracottaColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorTerracotta extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 1.25f;
    }

    @Override
    public float getResistance() {
        return 7;
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
        return TerracottaColor.getBlockColor(block.getState().getType());
    }

    public DyeColor getDyeColor(Block block) {
        return TerracottaColor.getDyeColor(block.getState().getType());
    }
}
