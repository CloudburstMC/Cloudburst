package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;

public class BlockBehaviorDispenser extends BlockBehaviorSolid {

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        /*BlockEntity blockEntity = this.world.getBlockEntity(this.getPosition());

        if(blockEntity instanceof BlockEntityDispenser) {
            //return ContainerInventory.calculateRedstone(((BlockEntityDispenser) blockEntity).getInventory()); TODO: dispenser
        }*/

        return super.getComparatorInputOverride(block);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    public Vector3f getDispensePosition() {
        return Vector3f.ZERO; //redstone update
    }
}
