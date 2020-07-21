package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.impl.vehicle.EntityAbstractMinecart;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;

import static org.cloudburstmc.server.block.BlockTypes.DETECTOR_RAIL;

public class BlockBehaviorRailDetector extends BlockBehaviorRail {

    public BlockBehaviorRailDetector() {
        canBePowered = true;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public int getWeakPower(BlockFace side) {
        return isActive() ? 15 : 0;
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return isActive() ? 0 : (side == BlockFace.UP ? 15 : 0);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            updateState();
            return type;
        }
        return super.onUpdate(block, type);
    }

    @Override
    public void onEntityCollide(Entity entity) {
        updateState();
    }

    protected void updateState() {
        boolean wasPowered = isActive();
        boolean isPowered = false;

        for (Entity entity : level.getNearbyEntities(new SimpleAxisAlignedBB(
                getX() + 0.125f,
                getY(),
                getZ() + 0.125f,
                getX() + 0.875f,
                getY() + 0.525f,
                getZ() + 0.875f))) {
            if (entity instanceof EntityAbstractMinecart) {
                isPowered = true;
            }
        }

        if (isPowered && !wasPowered) {
            setActive(true);
            level.scheduleUpdate(this, this.getPosition(), 0);
            level.scheduleUpdate(this, this.getPosition().down(), 0);
        }

        if (!isPowered && wasPowered) {
            setActive(false);
            level.scheduleUpdate(this, this.getPosition(), 0);
            level.scheduleUpdate(this, this.getPosition().down(), 0);
        }

        level.updateComparatorOutputLevel(this.getPosition());
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        return new Item[]{
                Item.get(DETECTOR_RAIL, 0, 1)
        };
    }
}
