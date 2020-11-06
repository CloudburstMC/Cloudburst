package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.REDSTONE_BLOCK;
import static org.cloudburstmc.server.block.BlockTypes.REDSTONE_WIRE;

public abstract class BlockBehaviorRedstoneDiode extends FloodableBlockBehavior {

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        Vector3i pos = block.getPosition();
        super.onBreak(block, item);

        for (Direction face : Direction.values()) {
            block.getLevel().updateAroundRedstone(face.getOffset(pos), null);
        }
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val downState = block.getSide(Direction.DOWN).getState();
        if (downState.getBehavior().isTransparent(downState)) {
            return false;
        }

        placeBlock(block, item.getBehavior().getBlock(item).withTrait(
                BlockTraits.DIRECTION,
                player != null ? player.getHorizontalDirection().getOpposite() : Direction.NORTH
        ));

        block = block.refresh();
        if (shouldBePowered(block)) {
            block.getLevel().scheduleUpdate(block, 1);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        Level level = block.getLevel();
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (!this.isLocked(block)) {
                Vector3i pos = block.getPosition();
                boolean shouldBePowered = this.shouldBePowered(block);
                boolean powered = isPowered(block.getState());
                val state = block.getState();

                if (powered && !shouldBePowered) {
                    block.set(this.getUnpowered(state), true, true);

                    level.updateAroundRedstone(this.getFacing(state).getOpposite().getOffset(pos), null);
                } else if (!powered) {
                    block.set(this.getPowered(state), true, true);
                    level.updateAroundRedstone(this.getFacing(state).getOpposite().getOffset(pos), null);

                    if (!shouldBePowered) {
//                        System.out.println("schedule update 2");
                        level.scheduleUpdate(level.getBlock(pos), pos, this.getDelay(state));
                    }
                }
            }
        } else if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent event = new RedstoneUpdateEvent(block);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) return 0;
            val downState = block.down().getState();
            if (type == Level.BLOCK_UPDATE_NORMAL && downState.getBehavior().isTransparent(downState)) {
                level.useBreakOn(block.getPosition());
            } else {
                this.updateState(block);
            }
            return Level.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }

    public void updateState(Block block) {
        if (!this.isLocked(block)) {

            Level level = block.getLevel();
            if (this.isPowered(block.getState()) != this.shouldBePowered(block) &&
                    !level.isBlockTickPending(block.getPosition(), block)) {
                /*int priority = -1;

                if (this.isFacingTowardsRepeater()) {
                    priority = -3;
                } else if (this.isPowered) {
                    priority = -2;
                }*/

                level.scheduleUpdate(block, block.getPosition(), this.getDelay(block.getState()));
            }
        }
    }

    public boolean isLocked(Block block) {
        return false;
    }

    protected int calculateInputStrength(Block block) {
        Direction face = getFacing(block.getState());
        Vector3i pos = face.getOffset(block.getPosition());
        int power = block.getLevel().getRedstonePower(pos, face);

        if (power >= 15) {
            return power;
        } else {
            val state = block.getLevel().getBlock(pos).getState();
            return Math.max(power, state.getType() == REDSTONE_WIRE ? state.ensureTrait(BlockTraits.REDSTONE_SIGNAL) : 0);
        }
    }

    protected int getPowerOnSides(Block block) {
        Vector3i pos = block.getPosition();

        Direction face = getFacing(block.getState());
        Direction face1 = face.rotateY();
        Direction face2 = face.rotateYCCW();
        return Math.max(this.getPowerOnSide(block, face1.getOffset(pos), face1), this.getPowerOnSide(block, face2.getOffset(pos), face2));
    }

    protected int getPowerOnSide(Block block, Vector3i pos, Direction side) {
        val b = block.getLevel().getBlock(pos);
        val state = b.getState();
        return isAlternateInput(b) ? (state.getType() == REDSTONE_BLOCK ? 15 :
                (state.getType() == REDSTONE_WIRE ? state.ensureTrait(BlockTraits.REDSTONE_SIGNAL)
                        : block.getLevel().getStrongPower(pos, side))) : 0;
    }


    protected boolean shouldBePowered(Block block) {
        return this.calculateInputStrength(block) > 0;
    }

    public Direction getFacing(BlockState state) {
        return state.ensureTrait(BlockTraits.DIRECTION);
    }

    protected abstract int getDelay(BlockState state);

    protected BlockState getPowered(BlockState state) {
        return state.withTrait(BlockTraits.IS_POWERED, true);
    }

    protected BlockState getUnpowered(BlockState state) {
        return state.withTrait(BlockTraits.IS_POWERED, false);
    }

//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.125f;
//    }

    protected boolean isAlternateInput(Block block) {
        return block.getState().getBehavior().isPowerSource(block);
    }

    public static boolean isDiode(BlockBehavior behavior) {
        return behavior instanceof BlockBehaviorRedstoneDiode;
    }

    protected int getRedstoneSignal(Block block) {
        return 15;
    }

    public int getStrongPower(Block block, Direction side) {
        return getWeakPower(block, side);
    }

    public int getWeakPower(Block block, Direction side) {
        val state = block.getState();
        return !this.isPowered(state) ? 0 : (getFacing(state) == side ? this.getRedstoneSignal(block) : 0);
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    public boolean isPowered(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED);
    }

    public boolean isFacingTowardsRepeater(Block block) {
        Direction side = getFacing(block.getState()).getOpposite();
        BlockState blockState = block.getSide(side).getState();
        return isDiode(blockState.getBehavior()) && blockState.ensureTrait(BlockTraits.DIRECTION) != side;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }


}
