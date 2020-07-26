package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;

public class BlockBehaviorRedstoneTorchUnlit extends BlockBehaviorTorch {

    @Override
    public int getLightLevel() {
        return 0;
    }

    @Override
    public int getWeakPower(Direction side) {
        return 0;
    }

    @Override
    public int getStrongPower(Direction side) {
        return 0;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(BlockTypes.REDSTONE_TORCH);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (super.onUpdate(block, type) == 0) {
            if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
                this.level.scheduleUpdate(this, tickRate());
            } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
                RedstoneUpdateEvent ev = new RedstoneUpdateEvent(this);
                getLevel().getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    return 0;
                }

                if (checkState()) {
                    return 1;
                }
            }
        }

        return 0;
    }

    protected boolean checkState() {
        Direction face = getBlockFace().getOpposite();
        Vector3i pos = this.getPosition();

        if (!this.level.isSidePowered(face.getOffset(pos), face)) {
            this.level.setBlock(pos, BlockState.get(BlockTypes.REDSTONE_TORCH, getMeta()), false, true);

            for (Direction side : Direction.values()) {
                if (side == face) {
                    continue;
                }

                this.level.updateAroundRedstone(side.getOffset(pos), null);
            }
            return true;
        }

        return false;
    }

    @Override
    public int tickRate() {
        return 2;
    }
}
