package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorIcePacked extends BlockBehaviorIce {

    public BlockBehaviorIcePacked(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int onUpdate(int type) {
        return 0; //not being melted
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        return this.getLevel().setBlock(this.getPosition(), BlockState.AIR, true); //no water
    }

    @Override
    public boolean onBreak(Item item, Player player) {
        return this.onBreak(item);
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
