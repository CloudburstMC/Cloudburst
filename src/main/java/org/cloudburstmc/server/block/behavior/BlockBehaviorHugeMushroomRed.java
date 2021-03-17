package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.api.block.BlockTypes.RED_MUSHROOM;

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
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (ThreadLocalRandom.current().nextInt(0, 20) == 0) {
            return new ItemStack[]{
                    CloudItemRegistry.get().getItem(RED_MUSHROOM)
            };
        } else {
            return new ItemStack[0];
        }
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.RED_BLOCK_COLOR;
    }
}
