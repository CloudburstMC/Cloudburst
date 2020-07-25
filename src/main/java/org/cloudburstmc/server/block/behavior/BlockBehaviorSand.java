package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorSand extends BlockBehaviorFallable {

    public static final int DEFAULT = 0;
    public static final int RED = 1;

    public BlockBehaviorSand(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.SAND_BLOCK_COLOR;
    }
}
