package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Pub4Game on 03.01.2015.
 */
public class BlockBehaviorMushroomRed extends BlockBehaviorMushroom {

    public BlockBehaviorMushroomRed(Identifier id) {
        super(id);
    }

    @Override
    protected int getType() {
        return 1;
    }
}
