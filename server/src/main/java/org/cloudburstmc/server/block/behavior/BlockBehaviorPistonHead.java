package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;

public class BlockBehaviorPistonHead extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[0];
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);
        BlockState piston = getSide(getFacing().getOpposite());

        if (piston instanceof BlockBehaviorPistonBase && ((BlockBehaviorPistonBase) piston).getFacing() == this.getFacing()) {
            piston.onBreak(item);
        }
        return true;
    }

    public BlockFace getFacing() {
        return BlockFace.fromIndex(this.getMeta()).getOpposite();
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(BlockTypes.AIR, 0, 0);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
