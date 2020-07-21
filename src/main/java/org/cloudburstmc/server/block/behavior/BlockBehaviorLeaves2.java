package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;

public class BlockBehaviorLeaves2 extends BlockBehaviorLeaves {
    public static final int ACACIA = 0;
    public static final int DARK_OAK = 1;

    @Override
    protected boolean canDropApple() {
        return (this.getMeta() & 0x01) != 0;
    }

    @Override
    protected Item getSapling() {
        return Item.get(BlockTypes.SAPLING, (this.getMeta() & 0x01) + 4);
    }
}
