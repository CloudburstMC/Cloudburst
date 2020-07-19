package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorFurnace extends BlockBehaviorFurnaceBurning {

    protected BlockBehaviorFurnace(Identifier id, BlockEntityType<? extends Furnace> furnace) {
        super(id, furnace);
    }

    @Override
    public int getLightLevel() {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}