package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Faceable;

import static org.cloudburstmc.server.block.BlockTypes.REDSTONE_BLOCK;
import static org.cloudburstmc.server.block.BlockTypes.REDSTONE_WIRE;

public abstract class BlockBehaviorRedstoneDiode extends FloodableBlockBehavior implements Faceable {

    protected boolean isPowered = false;

    @Override
    public boolean onBreak(Block block, Item item) {
        Vector3i pos = this.getPosition();
        super.onBreak(block, item);

        for (BlockFace face : BlockFace.values()) {
            this.level.updateAroundRedstone(face.getOffset(pos), null);
        }
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (block.getSide(BlockFace.DOWN).isTransparent()) {
            return false;
        }

        this.setMeta(player != null ? player.getDirection().getOpposite().getHorizontalIndex() : 0);
        this.level.setBlock(block.getPosition(), this, true, true);

        if (shouldBePowered()) {
            this.level.scheduleUpdate(this, 1);
        }
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (!this.isLocked()) {
                Vector3i pos = this.getPosition();
                boolean shouldBePowered = this.shouldBePowered();

                if (this.isPowered && !shouldBePowered) {
                    this.level.setBlock(pos, this.getUnpowered(), true, true);

                    this.level.updateAroundRedstone(this.getFacing().getOpposite().getOffset(pos), null);
                } else if (!this.isPowered) {
                    this.level.setBlock(pos, this.getPowered(), true, true);
                    this.level.updateAroundRedstone(this.getFacing().getOpposite().getOffset(pos), null);

                    if (!shouldBePowered) {
//                        System.out.println("schedule update 2");
                        level.scheduleUpdate(getPowered(), pos, this.getDelay());
                    }
                }
            }
        } else if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE) {
            // Redstone event
            RedstoneUpdateEvent ev = new RedstoneUpdateEvent(this);
            getLevel().getServer().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                return 0;
            }
            if (type == Level.BLOCK_UPDATE_NORMAL && this.getSide(BlockFace.DOWN).isTransparent()) {
                this.level.useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            } else {
                this.updateState();
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    public void updateState() {
        if (!this.isLocked()) {
            boolean shouldPowered = this.shouldBePowered();

            if ((this.isPowered && !shouldPowered || !this.isPowered && shouldPowered) &&
                    !this.level.isBlockTickPending(this.getPosition(), this)) {
                /*int priority = -1;

                if (this.isFacingTowardsRepeater()) {
                    priority = -3;
                } else if (this.isPowered) {
                    priority = -2;
                }*/

                this.level.scheduleUpdate(this, this.getPosition(), this.getDelay());
            }
        }
    }

    public boolean isLocked() {
        return false;
    }

    protected int calculateInputStrength() {
        BlockFace face = getFacing();
        Vector3i pos = face.getOffset(this.getPosition());
        int power = this.level.getRedstonePower(pos, face);

        if (power >= 15) {
            return power;
        } else {
            BlockState blockState = this.level.getBlock(pos);
            return Math.max(power, blockState.getId() == REDSTONE_WIRE ? blockState.getMeta() : 0);
        }
    }

    protected int getPowerOnSides() {
        Vector3i pos = this.getPosition();

        BlockFace face = getFacing();
        BlockFace face1 = face.rotateY();
        BlockFace face2 = face.rotateYCCW();
        return Math.max(this.getPowerOnSide(face1.getOffset(pos), face1), this.getPowerOnSide(face2.getOffset(pos), face2));
    }

    protected int getPowerOnSide(Vector3i pos, BlockFace side) {
        BlockState blockState = this.level.getBlock(pos);
        return isAlternateInput(blockState) ? (blockState.getId() == REDSTONE_BLOCK ? 15 : (blockState.getId() == REDSTONE_WIRE ? blockState.getMeta() : this.level.getStrongPower(pos, side))) : 0;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    protected boolean shouldBePowered() {
        return this.calculateInputStrength() > 0;
    }

    public abstract BlockFace getFacing();

    protected abstract int getDelay();

    protected abstract BlockState getUnpowered();

    protected abstract BlockState getPowered();

    @Override
    public float getMaxY() {
        return this.getY() + 0.125f;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    protected boolean isAlternateInput(BlockState blockState) {
        return blockState.isPowerSource();
    }

    public static boolean isDiode(BlockState blockState) {
        return blockState instanceof BlockBehaviorRedstoneDiode;
    }

    protected int getRedstoneSignal() {
        return 15;
    }

    public int getStrongPower(BlockFace side) {
        return getWeakPower(side);
    }

    public int getWeakPower(BlockFace side) {
        return !this.isPowered() ? 0 : (getFacing() == side ? this.getRedstoneSignal() : 0);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    public boolean isPowered() {
        return isPowered;
    }

    public boolean isFacingTowardsRepeater() {
        BlockFace side = getFacing().getOpposite();
        BlockState blockState = this.getSide(side);
        return blockState instanceof BlockBehaviorRedstoneDiode && ((BlockBehaviorRedstoneDiode) blockState).getFacing() != side;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public BlockColor getColor() {
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
