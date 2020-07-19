package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/11/25 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorStairsStoneBrick extends BlockBehaviorStairs {
    public BlockBehaviorStairsStoneBrick(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public float getHardness() {
        return 1.5f;
    }

    @Override
    public float getResistance() {
        return 30;
    }
}
