package org.cloudburstmc.server.block.behavior;

import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;

import static org.cloudburstmc.server.block.BlockTypes.DETECTOR_RAIL;

public class BlockBehaviorRailDetector extends BlockBehaviorRail {

    public BlockBehaviorRailDetector() {
        super(DETECTOR_RAIL, BlockTraits.SIMPLE_RAIL_DIRECTION);
        canBePowered = true;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction side) {
        return isActive(block.getState()) ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return isActive(block.getState()) ? 0 : (side == Direction.UP ? 15 : 0);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            updateState(block);
            return type;
        }
        return super.onUpdate(block, type);
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        updateState(block);
    }

    protected void updateState(Block block) {
        boolean wasPowered = isActive(block.getState());
        boolean isPowered = false;

        for (Entity entity : block.getLevel().getNearbyEntities(new SimpleAxisAlignedBB(
                block.getX() + 0.125f,
                block.getY(),
                block.getZ() + 0.125f,
                block.getX() + 0.875f,
                block.getY() + 0.525f,
                block.getZ() + 0.875f))) {
            if (entity instanceof EntityAbstractMinecart) {
                isPowered = true;
                break;
            }
        }

        val level = block.getLevel();

        if (isPowered && !wasPowered) {
            setActive(block, true);
            level.scheduleUpdate(block.getPosition(), 0);
            level.scheduleUpdate(block.getPosition().down(), 0);
        }

        if (!isPowered && wasPowered) {
            setActive(block, false);
            level.scheduleUpdate(block.getPosition(), 0);
            level.scheduleUpdate(block.getPosition().down(), 0);
        }

        level.updateComparatorOutputLevel(block.getPosition());
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(DETECTOR_RAIL, 0, 1)
        };
    }
}
