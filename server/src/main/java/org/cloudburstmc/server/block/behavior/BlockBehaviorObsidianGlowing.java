package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;

import static org.cloudburstmc.server.block.BlockTypes.OBSIDIAN;

public class BlockBehaviorObsidianGlowing extends BlockBehaviorSolid {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 50;
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public int getLightLevel() {
        return 12;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(OBSIDIAN);
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() > ItemTool.TIER_DIAMOND) {
            return new Item[]{
                    toItem(blockState)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}
