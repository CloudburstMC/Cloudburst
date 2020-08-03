package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorVine extends BlockBehaviorTransparent {
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int NORTH = 4;
    public static final int EAST = 8;

    public static Direction getFace(int meta) {
        if ((meta & EAST) != 0) {
            return Direction.EAST;
        } else if ((meta & NORTH) != 0) {
            return Direction.NORTH;
        } else if ((meta & WEST) != 0) {
            return Direction.WEST;
        } else {
            return Direction.SOUTH;
        }
    }

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public float getResistance() {
        return 1;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean canBeReplaced(Block block) {
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

    @Override
    public boolean isSolid() {
        return false;
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
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().inCategory(BlockCategory.SOLID) && face.getHorizontalIndex() != -1) {
            var state = BlockState.get(BlockTypes.VINE);

            if (face.getAxis().isHorizontal()) {
                state = state.withTrait(BlockTraits.DIRECTION, face.getOpposite());
            }

            placeBlock(block, state);
            return true;
        }

        return false;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isShears()) {
            return new Item[]{
                    toItem(block)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().defaultState());
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            val direction = block.getState().ensureTrait(BlockTraits.DIRECTION);

            if (!block.getSideState(direction).inCategory(BlockCategory.SOLID)) {
                val up = block.upState();

                if (up.getType() != BlockTypes.VINE || up.ensureTrait(BlockTraits.DIRECTION) != direction) {
                    block.getLevel().useBreakOn(block.getPosition());
                    return Level.BLOCK_UPDATE_NORMAL;
                }
            }
//            int current = block.getState().ensureTrait(BlockTraits.VINE_DIRECTION_BITS);
//
//            int bits = 0;
//            for (Direction direction : Plane.HORIZONTAL) {
//                if (block.getSide(direction).getState().inCategory(BlockCategory.SOLID)) {
//                    bits |= 1 << direction.getHorizontalIndex();
//                }
//            }
//
//            if (bits == 0) {
//                val upState = block.up().getState();
//                if (upState.getType() != BlockTypes.VINE || (upState.ensureTrait(BlockTraits.VINE_DIRECTION_BITS) & current) == 0) {
//                    block.getLevel().useBreakOn(block.getPosition(), null, null, true);
//                    return Level.BLOCK_UPDATE_NORMAL;
//                }
//            } else if (bits != current) {
//                block.set(block.getState().withTrait(BlockTraits.VINE_DIRECTION_BITS, bits));
//                return Level.BLOCK_UPDATE_NORMAL;
//            }
        }
        return 0;
    }

    public int getBits(Direction direction) {
        return 1 << direction.getHorizontalIndex();
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHEARS;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
