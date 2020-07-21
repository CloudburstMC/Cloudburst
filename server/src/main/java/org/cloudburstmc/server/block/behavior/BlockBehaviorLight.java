package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.AxisAlignedBB;

public class BlockBehaviorLight extends BlockBehaviorTransparent {

    @Override
    public void setMeta(int meta) {
        super.setMeta(meta & 0xF);
    }

    @Override
    public int getLightLevel() {
        return getMeta() & 0xF;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[0];
    }
}
