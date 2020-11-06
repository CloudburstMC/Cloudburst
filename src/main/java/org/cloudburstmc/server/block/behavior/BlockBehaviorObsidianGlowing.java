package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;

import static org.cloudburstmc.server.block.BlockTypes.OBSIDIAN;

public class BlockBehaviorObsidianGlowing extends BlockBehaviorSolid {


    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(OBSIDIAN);
    }

    @Override
    public boolean canBePushed() {
        return false;
    }


}
