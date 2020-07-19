package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/1 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorLeaves2 extends BlockBehaviorLeaves {
    public static final int ACACIA = 0;
    public static final int DARK_OAK = 1;

    public BlockBehaviorLeaves2(Identifier id) {
        super(id);
    }

    @Override
    protected boolean canDropApple() {
        return (this.getMeta() & 0x01) != 0;
    }

    @Override
    protected Item getSapling() {
        return Item.get(BlockTypes.SAPLING, (this.getMeta() & 0x01) + 4);
    }
}
