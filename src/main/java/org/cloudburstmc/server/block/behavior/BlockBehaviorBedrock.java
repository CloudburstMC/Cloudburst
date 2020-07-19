package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorBedrock extends BlockBehaviorSolid {

    public BlockBehaviorBedrock(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return -1;
    }

    @Override
    public float getResistance() {
        return 18000000;
    }

    @Override
    public boolean isBreakable(Item item) {
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
