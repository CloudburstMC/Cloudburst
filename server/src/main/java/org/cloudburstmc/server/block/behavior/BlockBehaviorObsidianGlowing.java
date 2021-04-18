package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.OBSIDIAN;

public class BlockBehaviorObsidianGlowing extends BlockBehaviorSolid {


    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(OBSIDIAN);
    }

    @Override
    public boolean canBePushed() {
        return false;
    }


}
