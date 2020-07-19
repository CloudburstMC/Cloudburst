package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.utils.Identifier;

/**
 * @author CreeperFace
 */
public class BlockBehaviorPistonHead extends BlockBehaviorTransparent {

    public BlockBehaviorPistonHead(Identifier id) {
        super(id);
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
    public Item[] getDrops(Item hand) {
        return new Item[0];
    }

    @Override
    public boolean onBreak(Item item) {
        super.onBreak(item);
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
    public Item toItem() {
        return Item.get(BlockTypes.AIR, 0, 0);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
