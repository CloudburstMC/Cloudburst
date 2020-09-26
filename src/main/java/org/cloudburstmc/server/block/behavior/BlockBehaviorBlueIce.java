package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.ToolTypes;
import org.cloudburstmc.server.player.Player;

public class BlockBehaviorBlueIce extends BlockBehaviorIce {

    @Override
    public ToolType getToolType() {
        return ToolTypes.PICKAXE;
    }

    @Override
    public float getHardness() {
        return 2.8f;
    }

    @Override
    public float getResistance() {
        return 14;
    }

    @Override
    public float getFrictionFactor() {
        return 0.989f;
    }

    @Override
    public int onUpdate(Block block, int type) {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        block.set(BlockStates.AIR);
        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item, Player player) {
        return this.onBreak(block, item);
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public boolean isTransparent() {
        return false;
    }


    @Override
    public int getLightLevel(Block block) {
        return 4;
    }
}
