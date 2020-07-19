package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.PORTAL;

/**
 * Created on 2015/12/2 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorObsidian extends BlockBehaviorSolid {

    public BlockBehaviorObsidian(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 35; //50 in PC
    }

    @Override
    public float getResistance() {
        return 6000;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_DIAMOND) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean onBreak(Item item) {
        //destroy the nether portal
        BlockState[] nearby = new BlockState[]{
                this.up(), this.down(),
                this.north(), south(),
                this.west(), this.east(),
        };
        for (BlockState aNearby : nearby) {
            if (aNearby != null) if (aNearby.getId() == PORTAL) {
                aNearby.onBreak(item);
            }
        }
        return super.onBreak(item);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.OBSIDIAN_BLOCK_COLOR;
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
