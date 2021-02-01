package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.block.Block;

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
