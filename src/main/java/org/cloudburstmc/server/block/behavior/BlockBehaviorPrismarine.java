package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorPrismarine extends BlockBehaviorSolid {

    public static final int NORMAL = 0;
    public static final int BRICKS = 1;
    public static final int DARK = 2;

    public BlockBehaviorPrismarine(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public BlockColor getColor() {
        switch (getMeta() & 0x07) {
            case NORMAL:
                return BlockColor.CYAN_BLOCK_COLOR;
            case BRICKS:
            case DARK:
                return BlockColor.DIAMOND_BLOCK_COLOR;
            default:
                return BlockColor.STONE_BLOCK_COLOR;
        }
    }
}
