package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorWaterLily extends FloodableBlockBehavior {

    @Override
    public float getMinX() {
        return this.getX() + 0.0625f;
    }

    @Override
    public float getMinZ() {
        return this.getZ() + 0.0625f;
    }

    @Override
    public float getMaxX() {
        return this.getX() + 0.9375f;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.015625f;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + 0.9375f;
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return this;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target instanceof BlockBehaviorWater) {
            BlockState up = target.up();
            if (up.getId() == AIR) {
                this.getLevel().setBlock(up.getPosition(), this, true, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (!(this.down() instanceof BlockBehaviorWater)) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }
}
