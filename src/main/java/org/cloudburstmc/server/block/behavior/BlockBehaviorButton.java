package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockCategory;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.event.block.BlockRedstoneEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;

public class BlockBehaviorButton extends FloodableBlockBehavior {

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().inCategory(BlockCategory.TRANSPARENT)) {
            return false;
        }

        BlockState btn = item.getBehavior().getBlock(item).withTrait(BlockTraits.FACING_DIRECTION, face);
        placeBlock(block, btn);
        return true;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (this.isActivated(block)) {
            return false;
        }

        CloudLevel level = block.getLevel();
        level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));

        block.set(block.getState().withTrait(BlockTraits.IS_BUTTON_PRESSED, true), true, false);

        level.addSound(block.getPosition(), Sound.RANDOM_CLICK);
        level.scheduleUpdate(block, 30);

        level.updateAroundRedstone(block.getPosition(), null);
        level.updateAroundRedstone(this.getFacing(block).getOpposite().getOffset(block.getPosition()), null);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (block.getSide(getFacing(block).getOpposite()).getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getLevel().useBreakOn(block.getPosition());
                return CloudLevel.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED) {
            if (this.isActivated(block)) {
                CloudLevel level = block.getLevel();
                level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));

                block.set(block.getState().withTrait(BlockTraits.IS_BUTTON_PRESSED, false),
                        true, false);
                level.addSound(block.getPosition(), Sound.RANDOM_CLICK);

                level.updateAroundRedstone(block.getPosition(), null);
                level.updateAroundRedstone(this.getFacing(block).getOpposite().getOffset(block.getPosition()), null);
            }

            return CloudLevel.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    public boolean isActivated(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_BUTTON_PRESSED);
    }



    public int getWeakPower(Block block, Direction side) {
        return isActivated(block) ? 15 : 0;
    }

    public int getStrongPower(Block block, Direction side) {
        return !isActivated(block) ? 0 : (getFacing(block) == side ? 15 : 0);
    }

    public Direction getFacing(Block block) {
        return block.getState().ensureTrait(BlockTraits.FACING_DIRECTION);
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        if (isActivated(block)) {
            block.getLevel().getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));
        }

        return super.onBreak(block, item);
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState().resetTrait(BlockTraits.FACING_DIRECTION).resetTrait(BlockTraits.IS_BUTTON_PRESSED));
    }


}
