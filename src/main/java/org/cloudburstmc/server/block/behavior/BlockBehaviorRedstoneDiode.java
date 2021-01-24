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
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.REDSTONE_BLOCK;
import static org.cloudburstmc.server.block.BlockIds.REDSTONE_WIRE;

public abstract class BlockBehaviorRedstoneDiode extends FloodableBlockBehavior {

    protected final Identifier type;
    protected boolean isPowered = false;

    public BlockBehaviorRedstoneDiode(Identifier type) {
        this.type = type;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        Vector3i pos = block.getPosition();
        super.onBreak(block, item);

        for (Direction face : Direction.values()) {
            block.getWorld().updateAroundRedstone(face.getOffset(pos), null);
        }
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.getSide(Direction.DOWN).getState().getBehavior().isTransparent()) {
            return false;
        }

        placeBlock(block, BlockState.get(this.type).withTrait(
                BlockTraits.DIRECTION,
                player != null ? player.getHorizontalDirection().getOpposite() : Direction.NORTH
        ));

        block = block.refresh();
        if (shouldBePowered(block)) {
            block.getWorld().scheduleUpdate(block, 1);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        World world = block.getWorld();
        if (type == World.BLOCK_UPDATE_SCHEDULED) {
            if (!this.isLocked(block)) {
                Vector3i pos = block.getPosition();
                boolean shouldBePowered = this.shouldBePowered(block);
                val state = block.getState();

                if (this.isPowered && !shouldBePowered) {
                    block.set(this.getUnpowered(state), true, true);

                    world.updateAroundRedstone(this.getFacing(state).getOpposite().getOffset(pos), null);
                } else if (!this.isPowered) {
                    block.set(this.getPowered(state), true, true);
                    world.updateAroundRedstone(this.getFacing(state).getOpposite().getOffset(pos), null);

                    if (!shouldBePowered) {
//                        System.out.println("schedule update 2");
                        world.scheduleUpdate(world.getBlock(pos), pos, this.getDelay(state));
                    }
                }
            }
        } else if (type == World.BLOCK_UPDATE_NORMAL || type == World.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent event = new RedstoneUpdateEvent(block);
            world.getServer().getEventManager().fire(event);
            if (event.isCancelled()) return 0;
            if (type == World.BLOCK_UPDATE_NORMAL && block.down().getState().getBehavior().isTransparent()) {
                world.useBreakOn(block.getPosition());
            } else {
                this.updateState(block);
            }
            return World.BLOCK_UPDATE_NORMAL;
        }
        return 0;
    }

    public void updateState(Block block) {
        if (!this.isLocked(block)) {
            boolean shouldPowered = this.shouldBePowered(block);

            World world = block.getWorld();
            if ((this.isPowered && !shouldPowered || !this.isPowered && shouldPowered) &&
                    !world.isBlockTickPending(block.getPosition(), block)) {
                /*int priority = -1;

                if (this.isFacingTowardsRepeater()) {
                    priority = -3;
                } else if (this.isPowered) {
                    priority = -2;
                }*/

                world.scheduleUpdate(block, block.getPosition(), this.getDelay(block.getState()));
            }
        }
    }

    public boolean isLocked(Block block) {
        return false;
    }

    protected int calculateInputStrength(Block block) {
        Direction face = getFacing(block.getState());
        Vector3i pos = face.getOffset(block.getPosition());
        int power = block.getWorld().getRedstonePower(pos, face);

        if (power >= 15) {
            return power;
        } else {
            val state = block.getWorld().getBlock(pos).getState();
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
        val b = block.getWorld().getBlock(pos);
        val state = b.getState();
        return isAlternateInput(b) ? (state.getType() == REDSTONE_BLOCK ? 15 :
                (state.getType() == REDSTONE_WIRE ? state.ensureTrait(BlockTraits.REDSTONE_SIGNAL)
                        : block.getWorld().getStrongPower(pos, side))) : 0;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    protected boolean shouldBePowered(Block block) {
        return this.calculateInputStrength(block) > 0;
    }

    public Direction getFacing(BlockState state) {
        return state.ensureTrait(BlockTraits.DIRECTION);
    }

    protected abstract int getDelay(BlockState state);

    protected abstract BlockState getUnpowered(BlockState state);

    protected abstract BlockState getPowered(BlockState state);

//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.125f;
//    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

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
        return isPowered;
    }

    public boolean isFacingTowardsRepeater(Block block) {
        Direction side = getFacing(block.getState()).getOpposite();
        BlockState blockState = block.getSide(side).getState();
        return blockState instanceof BlockBehaviorRedstoneDiode && ((BlockBehaviorRedstoneDiode) blockState).getFacing(blockState) != side;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }
}
