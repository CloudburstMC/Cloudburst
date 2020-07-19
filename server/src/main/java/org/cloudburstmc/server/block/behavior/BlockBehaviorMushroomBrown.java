package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * @author Nukkit Project Team
 */
public class BlockBehaviorMushroomBrown extends BlockBehaviorMushroom {

    public BlockBehaviorMushroomBrown(Identifier id) {
        super(id);
    }

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    protected int getType() {
        return 0;
    }
}
