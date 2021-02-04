package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import static org.cloudburstmc.api.block.BlockTypes.OBSIDIAN;

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
