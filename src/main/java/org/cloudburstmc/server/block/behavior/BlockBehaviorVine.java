package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Direction.Plane;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;

public class BlockBehaviorVine extends BlockBehaviorTransparent {

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean canBeClimbed() {
        return true;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        entity.resetFallDistance();
        entity.setOnGround(true);
    }


//    @Override
//    protected AxisAlignedBB recalculateBoundingBox() { //TODO: bounding box
//        float f1 = 1;
//        float f2 = 1;
//        float f3 = 1;
//        float f4 = 0;
//        float f5 = 0;
//        float f6 = 0;
//        boolean flag = this.getMeta() > 0;
//        if ((this.getMeta() & WEST) != 0) {
//            f4 = Math.max(f4, 0.0625f);
//            f1 = 0;
//            f2 = 0;
//            f5 = 1;
//            f3 = 0;
//            f6 = 1;
//            flag = true;
//        }
//        if ((this.getMeta() & EAST) != 0) {
//            f1 = Math.min(f1, 0.9375f);
//            f4 = 1;
//            f2 = 0;
//            f5 = 1;
//            f3 = 0;
//            f6 = 1;
//            flag = true;
//        }
//        if ((this.getMeta() & SOUTH) != 0) {
//            f3 = Math.min(f3, 0.9375f);
//            f6 = 1;
//            f1 = 0;
//            f4 = 1;
//            f2 = 0;
//            f5 = 1;
//            flag = true;
//        }
//        if (!flag && this.up().isSolid()) {
//            f2 = Math.min(f2, 0.9375f);
//            f5 = 1;
//            f1 = 0;
//            f4 = 1;
//            f3 = 0;
//            f6 = 1;
//        }
//        return new SimpleAxisAlignedBB(
//                this.getX() + f1,
//                this.getY() + f2,
//                this.getZ() + f3,
//                this.getX() + f4,
//                this.getY() + f5,
//                this.getZ() + f6
//        );
//    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().inCategory(BlockCategory.SOLID) && face.getAxis().isHorizontal()) {
            var state = BlockRegistry.get().getBlock(BlockTypes.VINE)
                    .withTrait(BlockTraits.VINE_DIRECTION_BITS, 1 << face.getOpposite().getHorizontalIndex());

            placeBlock(block, state);
            return true;
        }

        return false;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getBehavior().isShears()) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().defaultState());
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
//            val direction = block.getState().ensureTrait(BlockTraits.DIRECTION);
//
//            if (!block.getSideState(direction).inCategory(BlockCategory.SOLID)) {
//                val up = block.upState();
//
//                if (up.getType() != BlockTypes.VINE || up.ensureTrait(BlockTraits.DIRECTION) != direction) {
//                    block.getLevel().useBreakOn(block.getPosition());
//                    return Level.BLOCK_UPDATE_NORMAL;
//                }
//            }
            int current = block.getState().ensureTrait(BlockTraits.VINE_DIRECTION_BITS);

            int bits = 0;
            for (Direction direction : Plane.HORIZONTAL) {
                if (block.getSide(direction).getState().inCategory(BlockCategory.SOLID)) {
                    bits |= 1 << direction.getHorizontalIndex();
                }
            }

            if (bits == 0) {
                val upState = block.up().getState();
                if (upState.getType() != BlockTypes.VINE || (upState.ensureTrait(BlockTraits.VINE_DIRECTION_BITS) & current) == 0) {
                    block.getLevel().useBreakOn(block.getPosition(), null, null, true);
                    return CloudLevel.BLOCK_UPDATE_NORMAL;
                }
            } else if (bits != current) {
                block.set(block.getState().withTrait(BlockTraits.VINE_DIRECTION_BITS, bits));
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }


}
