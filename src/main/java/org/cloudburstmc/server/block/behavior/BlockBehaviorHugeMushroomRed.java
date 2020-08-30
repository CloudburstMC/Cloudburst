package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.RED_MUSHROOM;

public class BlockBehaviorHugeMushroomRed extends BlockBehaviorSolid {
    public static final int NONE = 0;
    public static final int TOP_NW = 1;
    public static final int TOP_N = 2;
    public static final int TOP_NE = 3;
    public static final int TOP_W = 4;
    public static final int TOP = 5;
    public static final int TOP_E = 6;
    public static final int TOP_SW = 7;
    public static final int TOP_S = 8;
    public static final int TOP_SE = 9;
    public static final int STEM = 10;
    public static final int ALL = 14;
    public static final int STEM_ALL = 15;

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public float getResistance() {
        return 1;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (ThreadLocalRandom.current().nextInt(0, 20) == 0) {
            return new Item[]{
                    Item.get(RED_MUSHROOM)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
