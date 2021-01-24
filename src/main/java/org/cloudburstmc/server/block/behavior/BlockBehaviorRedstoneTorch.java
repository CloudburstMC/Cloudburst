package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockIds.REDSTONE_TORCH;
import static org.cloudburstmc.server.block.BlockIds.UNLIT_REDSTONE_TORCH;

public class BlockBehaviorRedstoneTorch extends BlockBehaviorTorch {

    @Override
    public int getLightLevel(Block block) {
        return block.getState().getType() == REDSTONE_TORCH ? 7 : 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
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
//                this.world.updateAround(pos.getSide(side));
//            }
//        }

        checkState(block);
        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction side) {
        return block.getState().getType() == REDSTONE_TORCH && getBlockFace(block.getState()) != side ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return block.getState().getType() == REDSTONE_TORCH && side == Direction.DOWN ? this.getWeakPower(block, side) : 0;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);

        Vector3i pos = block.getPosition();

        Direction face = getBlockFace(block.getState()).getOpposite();

        for (Direction side : Direction.values()) {
            if (side == face) {
                continue;
            }

            block.getWorld().updateAroundRedstone(side.getOffset(pos), null);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (super.onUpdate(block, type) == 0) {
            if (type == World.BLOCK_UPDATE_NORMAL || type == World.BLOCK_UPDATE_REDSTONE) {
                block.getWorld().scheduleUpdate(block, tickRate());
            } else if (type == World.BLOCK_UPDATE_SCHEDULED) {
                RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
                block.getWorld().getServer().getEventManager().fire(ev);

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
        val type = block.getState().getType();
        boolean powered = isPoweredFromSide(block);

        if (powered && type == REDSTONE_TORCH || !powered && type == UNLIT_REDSTONE_TORCH) {
            Direction face = getBlockFace(block.getState()).getOpposite();
            Vector3i pos = block.getPosition();

            block.set(BlockState.get(type == REDSTONE_TORCH ? UNLIT_REDSTONE_TORCH : REDSTONE_TORCH).copyTrait(BlockTraits.TORCH_DIRECTION, block.getState()));

            for (Direction side : Direction.values()) {
                if (side == face) {
                    continue;
                }

                block.getWorld().updateAroundRedstone(side.getOffset(pos), null);
            }

            return true;
        }

        return false;
    }

    protected boolean isPoweredFromSide(Block block) {
        Direction face = getBlockFace(block.getState()).getOpposite();
        return block.getWorld().isSidePowered(face.getOffset(block.getPosition()), face);
    }

    @Override
    public int tickRate() {
        return 2;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}
