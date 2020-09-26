package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorWaterLily extends FloodableBlockBehavior {

//    @Override //TODO: bounding box
//    public float getMinX() {
//        return this.getX() + 0.0625f;
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.0625f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.015625f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.9375f;
//    }
//
//    @Override
//    protected AxisAlignedBB recalculateBoundingBox() {
//        return this;
//    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val targetType = target.getState().getType();
        if (targetType == BlockTypes.WATER || targetType == BlockTypes.FLOWING_WATER) {
            Block up = target.up();
            if (up.getState() == BlockStates.AIR) {
                placeBlock(block, BlockState.get(BlockTypes.WATERLILY));
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            val down = block.down().getState().getType();

            if (down != BlockTypes.WATER && down != BlockTypes.FLOWING_WATER) {
                block.getLevel().useBreakOn(block.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState());
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }
}
