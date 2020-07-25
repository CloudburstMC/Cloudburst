package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.math.Direction;

public class BlockBehaviorDispenser extends BlockBehaviorSolid {

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        /*BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if(blockEntity instanceof BlockEntityDispenser) {
            //return ContainerInventory.calculateRedstone(((BlockEntityDispenser) blockEntity).getInventory()); TODO: dispenser
        }*/

        return super.getComparatorInputOverride();
    }

    public Direction getFacing() {
        return Direction.fromIndex(this.getMeta() & 7);
    }

    public boolean isTriggered() {
        return (this.getMeta() & 8) > 0;
    }

    public void setTriggered(boolean value) {
        int i = 0;
        i |= getFacing().getIndex();

        if (value) {
            i |= 8;
        }

        this.setMeta(i);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    public Vector3f getDispensePosition() {
        Direction facing = getFacing();
        float x = this.getX() + 0.7f * facing.getXOffset();
        float y = this.getY() + 0.7f * facing.getYOffset();
        float z = this.getZ() + 0.7f * facing.getZOffset();

        return Vector3f.from(x, y, z);
    }
}
