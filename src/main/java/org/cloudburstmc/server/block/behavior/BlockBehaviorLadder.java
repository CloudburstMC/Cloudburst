package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.LADDER;

public class BlockBehaviorLadder extends BlockBehaviorTransparent {

    public BlockBehaviorLadder() {
        calculateOffsets();
    }

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

    private void calculateOffsets() {
        float f = 0.1875f;

        switch (this.getMeta()) {
            case 2:
                this.offMinX = 0;
                this.offMinZ = 1 - f;
                this.offMaxX = 1;
                this.offMaxZ = 1;
                break;
            case 3:
                this.offMinX = 0;
                this.offMinZ = 0;
                this.offMaxX = 1;
                this.offMaxZ = f;
                break;
            case 4:
                this.offMinX = 1 - f;
                this.offMinZ = 0;
                this.offMaxX = 1;
                this.offMaxZ = 1;
                break;
            case 5:
                this.offMinX = 0;
                this.offMinZ = 0;
                this.offMaxX = f;
                this.offMaxZ = 1;
                break;
            default:
                this.offMinX = 0;
                this.offMinZ = 1;
                this.offMaxX = 1;
                this.offMaxZ = 1;
                break;
        }
    }

    @Override
    public void setMeta(int meta) {
        super.setMeta(meta);
        calculateOffsets();
    }

    @Override
    public float getMinX() {
        return this.getX() + offMinX;
    }

    @Override
    public float getMinZ() {
        return this.getZ() + offMinZ;
    }

    @Override
    public float getMaxX() {
        return this.getX() + offMaxX;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + offMaxZ;
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return super.recalculateBoundingBox();
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!target.isTransparent()) {
            if (face.getIndex() >= 2 && face.getIndex() <= 5) {
                this.setMeta(face.getIndex());
                this.getLevel().setBlock(blockState.getPosition(), this, true, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            int[] faces = {
                    0, //never use
                    1, //never use
                    3,
                    2,
                    5,
                    4
            };
            if (!this.getSide(Direction.fromIndex(faces[this.getMeta()])).isSolid()) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(LADDER, 0, 1)
        };
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
