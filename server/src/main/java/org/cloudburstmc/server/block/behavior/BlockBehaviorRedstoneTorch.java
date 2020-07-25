package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.UNLIT_REDSTONE_TORCH;

public class BlockBehaviorRedstoneTorch extends BlockBehaviorTorch {

    @Override
    public int getLightLevel() {
        return 7;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!super.place(item, blockState, target, face, clickPos, player)) {
            return false;
        }

//        if (!checkState()) {
//            BlockFace facing = getFacing().getOpposite();
//            Vector3 pos = getLocation();
//
//            for (BlockFace side : BlockFace.values()) {
//                if (facing == side) {
//                    continue;
//                }
//
//                this.level.updateAround(pos.getSide(side));
//            }
//        }

        checkState();

        return true;
    }

    @Override
    public int getWeakPower(Direction side) {
        return getBlockFace() != side ? 15 : 0;
    }

    @Override
    public int getStrongPower(Direction side) {
        return side == Direction.DOWN ? this.getWeakPower(side) : 0;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);

        Vector3i pos = this.getPosition();

        Direction face = getBlockFace().getOpposite();

        for (Direction side : Direction.values()) {
            if (side == face) {
                continue;
            }

            this.level.updateAroundRedstone(side.getOffset(pos), null);
        }
        return true;
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
        if (isPoweredFromSide()) {
            Direction face = getBlockFace().getOpposite();
            Vector3i pos = this.getPosition();

            this.level.setBlock(pos, BlockState.get(UNLIT_REDSTONE_TORCH, getMeta()), false, true);

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

    protected boolean isPoweredFromSide() {
        Direction face = getBlockFace().getOpposite();
        return this.level.isSidePowered(face.getOffset(this.getPosition()), face);
    }

    @Override
    public int tickRate() {
        return 2;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
