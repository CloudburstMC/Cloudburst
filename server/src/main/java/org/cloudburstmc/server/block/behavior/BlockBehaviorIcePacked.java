package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorIcePacked extends BlockBehaviorIce {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int onUpdate(Block block, int type) {
        return 0; //not being melted
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        return this.getLevel().setBlock(this.getPosition(), BlockState.AIR, true); //no water
    }

    @Override
    public boolean onBreak(Block block, Item item, Player player) {
        return this.onBreak(item);
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
