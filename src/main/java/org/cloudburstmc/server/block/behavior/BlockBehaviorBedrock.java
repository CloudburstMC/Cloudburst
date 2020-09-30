package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemStack;

public class BlockBehaviorBedrock extends BlockBehaviorSolid {


    @Override
    public float getResistance() {
        return 18000000;
    }

    @Override
    public boolean isBreakable(ItemStack item) {
        return false;
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
