package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/8 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorPumpkinLit extends BlockBehaviorPumpkin {
    public BlockBehaviorPumpkinLit(Identifier id) {
        super(id);
    }

    @Override
    public int getLightLevel() {
        return 15;
    }

}
