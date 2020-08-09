package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;

public class BlockBehaviorPistonHead extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[0];
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(BlockTypes.AIR, 0, 0);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
