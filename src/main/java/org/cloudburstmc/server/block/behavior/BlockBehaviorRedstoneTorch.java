package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.level.CloudLevel;

public class BlockBehaviorRedstoneTorch extends BlockBehaviorTorch {

    @Override
    public int getLightLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_POWERED) ? 7 : 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!super.place(item, block, target, face, clickPos, player)) {
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

        checkState(block);
        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction side) {
        return !block.getState().ensureTrait(BlockTraits.IS_POWERED) && getBlockFace(block.getState()) != side ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return !block.getState().ensureTrait(BlockTraits.IS_POWERED) && side == Direction.DOWN ? this.getWeakPower(block, side) : 0;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        super.onBreak(block, item);

        Vector3i pos = block.getPosition();

        Direction face = getBlockFace(block.getState()).getOpposite();

        for (Direction side : Direction.values()) {
            if (side == face) {
                continue;
            }

            ((CloudLevel) block.getLevel()).updateAroundRedstone(side.getOffset(pos), null);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (super.onUpdate(block, type) == 0) {
            if (type == CloudLevel.BLOCK_UPDATE_NORMAL || type == CloudLevel.BLOCK_UPDATE_REDSTONE) {
                block.getLevel().scheduleUpdate(block.getPosition(), tickRate());
            } else if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED) {
                RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
                block.getLevel().getServer().getEventManager().fire(ev);

                if (ev.isCancelled()) {
                    return 0;
                }

                if (checkState(block)) {
                    return 1;
                }
            }
        }

        return 0;
    }

    protected boolean checkState(Block block) {
        val state = block.getState();
        boolean powered = isPoweredFromSide(block);

        if (powered != state.ensureTrait(BlockTraits.IS_POWERED)) {
            Direction face = getBlockFace(state).getOpposite();
            Vector3i pos = block.getPosition();

            block.set(state.toggleTrait(BlockTraits.IS_POWERED));

            for (Direction side : Direction.values()) {
                if (side == face) {
                    continue;
                }

                ((CloudLevel) block.getLevel()).updateAroundRedstone(side.getOffset(pos), null);
            }

            return true;
        }

        return false;
    }

    protected boolean isPoweredFromSide(Block block) {
        Direction face = getBlockFace(block.getState()).getOpposite();
        return ((CloudLevel) block.getLevel()).isSidePowered(face.getOffset(block.getPosition()), face);
    }

    @Override
    public int tickRate() {
        return 2;
    }


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
