package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;

public class BlockBehaviorButtonStone extends BlockBehaviorButton {

    public BlockBehaviorButtonStone() {
        super(BlockTypes.STONE_BUTTON);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe()) {
            return super.getDrops(block, hand);
        } else {
            return new Item[0];
        }
    }
}
