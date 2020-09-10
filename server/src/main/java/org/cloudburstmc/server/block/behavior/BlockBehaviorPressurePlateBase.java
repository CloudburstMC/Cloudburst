package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.entity.EntityInteractEvent;
import org.cloudburstmc.server.event.player.PlayerInteractEvent;
import org.cloudburstmc.server.event.player.PlayerInteractEvent.Action;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

/**
 * Created by Snake1999 on 2016/1/11.
 * Package cn.nukkit.block in project nukkit
 */
public abstract class BlockBehaviorPressurePlateBase extends FloodableBlockBehavior {

    protected float onPitch;
    protected float offPitch;

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

//    @Override //TODO: bounding box
//    public float getMinX() {
//        return this.getX() + 0.625f;
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.625f;
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY() + 0;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxY() {
//        return isActivated() ? this.getY() + 0.03125f : this.getY() + 0.0625f;
//    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
            }
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            int power = this.getRedstonePower(block.getState());

            if (power > 0) {
                this.updateState(block, power);
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.down().getState().inCategory(BlockCategory.TRANSPARENT)) {
            return false;
        }

        return placeBlock(block, item);
    }

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateCollisionBoundingBox() {
//        return new SimpleAxisAlignedBB(this.getX() + 0.125f, this.getY(), this.getZ() + 0.125f, this.getX() + 0.875f, this.getY() + 0.25f, this.getZ() + 0.875f);
//    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        int power = getRedstonePower(block.getState());

        if (power == 0) {
            Event ev;

            if (entity instanceof Player) {
                ev = new PlayerInteractEvent((Player) entity, null, block, null, Action.PHYSICAL);
            } else {
                ev = new EntityInteractEvent(entity, block);
            }

            block.getLevel().getServer().getEventManager().fire(ev);

            if (!ev.isCancelled()) {
                updateState(block, power);
            }
        }
    }

    protected void updateState(Block block, int oldStrength) {
        int strength = this.computeRedstoneStrength(block);
        boolean wasPowered = oldStrength > 0;
        boolean isPowered = strength > 0;

        val level = block.getLevel();

        if (oldStrength != strength) {
            block.set(block.getState().withTrait(BlockTraits.REDSTONE_SIGNAL, strength), false, false);

            level.updateAroundRedstone(block.getPosition(), null);
            level.updateAroundRedstone(block.getPosition().down(), null);

            if (!isPowered && wasPowered) {
                this.playOffSound(block);
                level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));
            } else if (isPowered && !wasPowered) {
                this.playOnSound(block);
                level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));
            }
        }

        if (isPowered) {
            level.scheduleUpdate(block.getPosition(), 20);
        }
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        super.onBreak(block, item);

        if (this.getRedstonePower(block.getState()) > 0) {
            block.getLevel().updateAroundRedstone(block.getPosition(), null);
            block.getLevel().updateAroundRedstone(block.getPosition().down(), null);
        }

        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction side) {
        return getRedstonePower(block.getState());
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return side == Direction.UP ? this.getRedstonePower(block.getState()) : 0;
    }

    public int getRedstonePower(BlockState state) {
        return state.ensureTrait(BlockTraits.REDSTONE_SIGNAL);
    }

    protected void playOnSound(Block block) {
        block.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.POWER_ON);
    }

    protected void playOffSound(Block block) {
        block.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.POWER_OFF);
    }

    protected abstract int computeRedstoneStrength(Block block);

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
