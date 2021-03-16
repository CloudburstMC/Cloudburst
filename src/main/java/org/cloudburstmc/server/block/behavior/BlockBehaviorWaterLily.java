package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

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
                placeBlock(block, BlockRegistry.get().getBlock(BlockTypes.WATERLILY));
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            val down = block.down().getState().getType();

            if (down != BlockTypes.WATER && down != BlockTypes.FLOWING_WATER) {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState());
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }
}
