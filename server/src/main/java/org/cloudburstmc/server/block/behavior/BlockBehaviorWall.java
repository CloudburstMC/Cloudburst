package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;

import static org.cloudburstmc.server.block.BlockTypes.COBBLESTONE_WALL;

public class BlockBehaviorWall extends BlockBehaviorTransparent {
    public static final int NONE_MOSSY_WALL = 0;
    public static final int MOSSY_WALL = 1;


    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {

        boolean north = this.canConnect(this.getSide(Direction.NORTH));
        boolean south = this.canConnect(this.getSide(Direction.SOUTH));
        boolean west = this.canConnect(this.getSide(Direction.WEST));
        boolean east = this.canConnect(this.getSide(Direction.EAST));

        float n = north ? 0 : 0.25f;
        float s = south ? 1 : 0.75f;
        float w = west ? 0 : 0.25f;
        float e = east ? 1 : 0.75f;

        if (north && south && !west && !east) {
            w = 0.3125f;
            e = 0.6875f;
        } else if (!north && !south && west && east) {
            n = 0.3125f;
            s = 0.6875f;
        }

        return new SimpleAxisAlignedBB(
                this.getX() + w,
                this.getY(),
                this.getZ() + n,
                this.getX() + e,
                this.getY() + 1.5f,
                this.getZ() + s
        );
    }

    public boolean canConnect(BlockState blockState) {
        return (!(blockState.getId() != COBBLESTONE_WALL && blockState instanceof BlockBehaviorFence)) || blockState.isSolid() && !blockState.isTransparent();
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
