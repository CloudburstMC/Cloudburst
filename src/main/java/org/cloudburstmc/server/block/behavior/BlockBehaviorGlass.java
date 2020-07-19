package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class BlockBehaviorGlass extends BlockBehaviorTransparent {

    public BlockBehaviorGlass(Identifier id) {
        super(id);
    }

    @Override
    public float getResistance() {
        return 1.5f;
    }

    @Override
    public float getHardness() {
        return 0.3f;
    }

    @Override
    public Item[] getDrops(Item hand) {
        return new Item[0];
    }

    @Override
    public boolean canSilkTouch() {
        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
