package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public abstract class BlockBehaviorButton extends FloodableBlockBehavior {

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.isTransparent()) {
            return false;
        }

        this.setMeta(face.getIndex());
        this.level.setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (this.isActivated()) {
            return false;
        }

        this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 0, 15));
        this.setMeta(this.getMeta() ^ 0x08);
        this.level.setBlock(this.getPosition(), this, true, false);
        this.level.addSound(this.getPosition(), Sound.RANDOM_CLICK);
        this.level.scheduleUpdate(this, 30);

        level.updateAroundRedstone(this.getPosition(), null);
        level.updateAroundRedstone(this.getFacing().getOpposite().getOffset(this.getPosition()), null);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.getSide(getFacing().getOpposite()).isTransparent()) {
                this.level.useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (this.isActivated()) {
                this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 15, 0));

                this.setMeta(this.getMeta() ^ 0x08);
                this.level.setBlock(this.getPosition(), this, true, false);
                this.level.addSound(this.getPosition(), Sound.RANDOM_CLICK);

                level.updateAroundRedstone(this.getPosition(), null);
                level.updateAroundRedstone(this.getFacing().getOpposite().getOffset(this.getPosition()), null);
            }

            return Level.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    public boolean isActivated() {
        return ((this.getMeta() & 0x08) == 0x08);
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    public int getWeakPower(Direction side) {
        return isActivated() ? 15 : 0;
    }

    public int getStrongPower(Direction side) {
        return !isActivated() ? 0 : (getFacing() == side ? 15 : 0);
    }

    public Direction getFacing() {
        int side = isActivated() ? getMeta() ^ 0x08 : getMeta();
        return Direction.fromIndex(side);
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        if (isActivated()) {
            this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 15, 0));
        }

        return super.onBreak(block, item);
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(this.getId(), 0);
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x7);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
