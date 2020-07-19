package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.RED_FLOWER;

/**
 * Created on 2015/12/2 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public class BlockBehaviorDandelion extends BlockBehaviorFlower {
    public BlockBehaviorDandelion(Identifier id) {
        super(id);
    }

    @Override
    protected BlockState getUncommonFlower() {
        return BlockState.get(RED_FLOWER);
    }
}
