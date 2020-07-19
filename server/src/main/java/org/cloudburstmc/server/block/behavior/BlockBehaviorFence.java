package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created on 2015/12/7 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public abstract class BlockBehaviorFence extends BlockBehaviorTransparent {

    public BlockBehaviorFence(Identifier id) {
        super(id);
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        boolean north = this.canConnect(this.north());
        boolean south = this.canConnect(this.south());
        boolean west = this.canConnect(this.west());
        boolean east = this.canConnect(this.east());
        float n = north ? 0 : 0.375f;
        float s = south ? 1 : 0.625f;
        float w = west ? 0 : 0.375f;
        float e = east ? 1 : 0.625f;
        return new SimpleAxisAlignedBB(
                this.getX() + w,
                this.getY(),
                this.getZ() + n,
                this.getX() + e,
                this.getY() + 1.5f,
                this.getZ() + s
        );
    }

    public abstract boolean canConnect(BlockState blockState);

    @Override
    public BlockColor getColor() {
        switch (this.getMeta() & 0x07) {
            default:
            case 1: //OAK
                return BlockColor.WOOD_BLOCK_COLOR;
            case 2: //SPRUCE
                return BlockColor.SPRUCE_BLOCK_COLOR;
            case 3: //BIRCH
                return BlockColor.SAND_BLOCK_COLOR;
            case 4: //JUNGLE
                return BlockColor.DIRT_BLOCK_COLOR;
            case 5: //ACACIA
                return BlockColor.ORANGE_BLOCK_COLOR;
            case 6: //DARK OAK
                return BlockColor.BROWN_BLOCK_COLOR;
        }
    }

    @Override
    public Item toItem() {
        return Item.get(id, this.getMeta());
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
