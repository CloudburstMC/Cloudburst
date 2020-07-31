package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Furnace;

public class BlockBehaviorFurnace extends BlockBehaviorFurnaceBurning {

    public BlockBehaviorFurnace(BlockEntityType<? extends Furnace> entity) {
        super(entity);
    }

    @Override
    public int getLightLevel(Block block) {
        return 0;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}