package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;


public class BlockBehaviorSeaLantern extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 1.5f;
    }


    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(ItemTypes.PRISMARINE_CRYSTALS, 0, ThreadLocalRandom.current().nextInt(2, 4))
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }
}
