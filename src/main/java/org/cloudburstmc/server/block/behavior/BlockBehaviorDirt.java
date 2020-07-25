package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DirtType;

import static org.cloudburstmc.server.block.BlockTraits.DIRT_TYPE;
import static org.cloudburstmc.server.block.BlockTypes.DIRT;
import static org.cloudburstmc.server.block.BlockTypes.FARMLAND;

public class BlockBehaviorDirt extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.isHoe()) {
            item.useOn(block);
            block.set(BlockState.get(block.getState().ensureTrait(DIRT_TYPE) == DirtType.NORMAL ? FARMLAND : DIRT), true);
            return true;
        }

        return false;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{Item.get(BlockTypes.DIRT)};
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
