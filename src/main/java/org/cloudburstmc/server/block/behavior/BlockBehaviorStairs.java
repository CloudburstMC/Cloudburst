package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierTypes;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public abstract class BlockBehaviorStairs extends BlockBehaviorTransparent {

//    @Override //TODO: bounding box
//    public float getMinY() {
//        // TODO: this seems wrong
//        return this.getY() + ((getMeta() & 0x04) > 0 ? 0.5f : 0);
//    }
//
//    @Override
//    public float getMaxY() {
//        // TODO: this seems wrong
//        return this.getY() + ((getMeta() & 0x04) > 0 ? 1 : 0.5f);
//    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        var state = item.getBehavior().getBlock(item)
                .withTrait(BlockTraits.DIRECTION, player != null ? player.getDirection() : Direction.NORTH);
        if ((clickPos.getY() > 0.5 && face != Direction.UP) || face == Direction.DOWN) {
            state = state.withTrait(BlockTraits.IS_UPSIDE_DOWN, true);
        }

        placeBlock(block, state);
        return true;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        val behavior = hand.getBehavior();
        if (behavior.isPickaxe() && behavior.getTier(hand).compareTo(TierTypes.WOOD) >= 0) {
            return new ItemStack[]{
                    toItem(block)
            };
        } else {
            return new ItemStack[0];
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().resetTrait(BlockTraits.IS_UPSIDE_DOWN));
    }

//    @Override //TODO: bounding box
//    public boolean collidesWithBB(AxisAlignedBB bb) {
//        int damage = this.getMeta();
//        int side = damage & 0x03;
//        float f = 0;
//        float f1 = 0.5f;
//        float f2 = 0.5f;
//        float f3 = 1;
//        if ((damage & 0x04) > 0) {
//            f = 0.5f;
//            f1 = 1;
//            f2 = 0;
//            f3 = 0.5f;
//        }
//
//        if (bb.intersectsWith(new SimpleAxisAlignedBB(
//                this.getX(),
//                this.getY() + f,
//                this.getZ(),
//                this.getX() + 1,
//                this.getY() + f1,
//                this.getZ() + 1
//        ))) {
//            return true;
//        }
//
//
//        if (side == 0) {
//            if (bb.intersectsWith(new SimpleAxisAlignedBB(
//                    this.getX() + 0.5f,
//                    this.getY() + f2,
//                    this.getZ(),
//                    this.getX() + 1,
//                    this.getY() + f3,
//                    this.getZ() + 1
//            ))) {
//                return true;
//            }
//        } else if (side == 1) {
//            if (bb.intersectsWith(new SimpleAxisAlignedBB(
//                    this.getX(),
//                    this.getY() + f2,
//                    this.getZ(),
//                    this.getX() + 0.5f,
//                    this.getY() + f3,
//                    this.getZ() + 1
//            ))) {
//                return true;
//            }
//        } else if (side == 2) {
//            if (bb.intersectsWith(new SimpleAxisAlignedBB(
//                    this.getX(),
//                    this.getY() + f2,
//                    this.getZ() + 0.5f,
//                    this.getX() + 1,
//                    this.getY() + f3,
//                    this.getZ() + 1
//            ))) {
//                return true;
//            }
//        } else if (side == 3) {
//            if (bb.intersectsWith(new SimpleAxisAlignedBB(
//                    this.getX(),
//                    this.getY() + f2,
//                    this.getZ(),
//                    this.getX() + 1,
//                    this.getY() + f3,
//                    this.getZ() + 0.5f
//            ))) {
//                return true;
//            }
//        }
//
//        return false;
//    }


//    @Override
//    public float getHardness(BlockState blockState) { //TODO: stairs hardness
//        val type = blockState.getType();
//
//        if (type == BlockTypes.STAIRS)
//
//            return super.getHardness(blockState);
//    }


}
