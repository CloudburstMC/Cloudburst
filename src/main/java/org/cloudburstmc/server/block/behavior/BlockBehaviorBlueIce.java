package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorBlueIce extends BlockBehaviorIce {

    public BlockBehaviorBlueIce(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
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
    public int onUpdate(int type) {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return true;
    }

    @Override
    public boolean onBreak(Item item) {
        this.getLevel().setBlock(this.getPosition(), BlockState.AIR, true);
        return true;
    }

    @Override
    public boolean onBreak(Item item, Player player) {
        return this.onBreak(item);
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
    public int getLightLevel() {
        return 4;
    }
}
