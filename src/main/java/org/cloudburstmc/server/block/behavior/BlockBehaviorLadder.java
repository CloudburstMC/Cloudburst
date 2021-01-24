package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLadder extends BlockBehaviorTransparent {

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean canBeClimbed() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    private float offMinX;
    private float offMinZ;
    private float offMaxX;
    private float offMaxZ;

    @Override
    public float getHardness() {
        return 0.4f;
    }

    @Override
    public float getResistance() {
        return 2;
    }

//    private void calculateOffsets() { //TODO: bounding box
//        float f = 0.1875f;
//
//        switch (this.getMeta()) {
//            case 2:
//                this.offMinX = 0;
//                this.offMinZ = 1 - f;
//                this.offMaxX = 1;
//                this.offMaxZ = 1;
//                break;
//            case 3:
//                this.offMinX = 0;
//                this.offMinZ = 0;
//                this.offMaxX = 1;
//                this.offMaxZ = f;
//                break;
//            case 4:
//                this.offMinX = 1 - f;
//                this.offMinZ = 0;
//                this.offMaxX = 1;
//                this.offMaxZ = 1;
//                break;
//            case 5:
//                this.offMinX = 0;
//                this.offMinZ = 0;
//                this.offMaxX = f;
//                this.offMaxZ = 1;
//                break;
//            default:
//                this.offMinX = 0;
//                this.offMinZ = 1;
//                this.offMaxX = 1;
//                this.offMaxZ = 1;
//                break;
//        }
//    }
//
//    @Override
//    public float getMinX() {
//        return this.getX() + offMinX;
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + offMinZ;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + offMaxX;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + offMaxZ;
//    }
//
//    @Override
//    protected AxisAlignedBB recalculateCollisionBoundingBox() {
//        return super.recalculateBoundingBox();
//    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!target.getState().inCategory(BlockCategory.TRANSPARENT)) {
            if (face.getHorizontalIndex() != -1) {
                placeBlock(block, item.getBlock().withTrait(BlockTraits.FACING_DIRECTION, face));
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (!block.getSide(block.getState().ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite()).getState().inCategory(BlockCategory.SOLID)) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(block.getState().defaultState())
        };
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
