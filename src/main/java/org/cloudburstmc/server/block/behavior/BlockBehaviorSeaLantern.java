package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.item.ItemTypes;

import java.util.concurrent.ThreadLocalRandom;


public class BlockBehaviorSeaLantern extends BlockBehaviorTransparent {

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(ItemTypes.PRISMARINE_CRYSTALS, ThreadLocalRandom.current().nextInt(2, 4))
        };
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.QUARTZ_BLOCK_COLOR;
    }

}
