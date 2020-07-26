package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.Rail;

import static org.cloudburstmc.server.block.BlockTypes.ACTIVATOR_RAIL;

public class BlockBehaviorRailActivator extends BlockBehaviorRail {

    public BlockBehaviorRailActivator() {
        canBePowered = true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE || type == Level.BLOCK_UPDATE_SCHEDULED) {
            super.onUpdate(block, type);
            boolean wasPowered = isActive();
            boolean isPowered = level.isBlockPowered(this.getPosition())
                    || checkSurrounding(this.getPosition(), true, 0)
                    || checkSurrounding(this.getPosition(), false, 0);
            boolean hasUpdate = false;

            if (wasPowered != isPowered) {
                setActive(isPowered);
                hasUpdate = true;
            }

            if (hasUpdate) {
                level.updateAround(this.getPosition().down());
                if (getOrientation().isAscending()) {
                    level.updateAround(this.getPosition().up());
                }
            }
            return type;
        }
        return 0;
    }

    /**
     * Check the surrounding of the rail
     *
     * @param pos      The rail position
     * @param relative The relative of the rail that will be checked
     * @param power    The count of the rail that had been counted
     * @return Boolean of the surrounding area. Where the powered rail on!
     */
    protected boolean checkSurrounding(Vector3i pos, boolean relative, int power) {
        if (power >= 8) {
            return false;
        }
        int dx = pos.getX();
        int dy = pos.getY();
        int dz = pos.getZ();

        BlockBehaviorRail block;
        BlockState blockState2 = level.getBlock(dx, dy, dz);

        if (Rail.isRailBlock(blockState2)) {
            block = (BlockBehaviorRail) blockState2;
        } else {
            return false;
        }

        Rail.Orientation base = null;
        boolean onStraight = true;

        switch (block.getOrientation()) {
            case STRAIGHT_NORTH_SOUTH:
                if (relative) {
                    dz++;
                } else {
                    dz--;
                }
                break;
            case STRAIGHT_EAST_WEST:
                if (relative) {
                    dx--;
                } else {
                    dx++;
                }
                break;
            case ASCENDING_EAST:
                if (relative) {
                    dx--;
                } else {
                    dx++;
                    dy++;
                    onStraight = false;
                }
                base = Rail.Orientation.STRAIGHT_EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (relative) {
                    dx--;
                    dy++;
                    onStraight = false;
                } else {
                    dx++;
                }
                base = Rail.Orientation.STRAIGHT_EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (relative) {
                    dz++;
                } else {
                    dz--;
                    dy++;
                    onStraight = false;
                }
                base = Rail.Orientation.STRAIGHT_NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (relative) {
                    dz++;
                    dy++;
                    onStraight = false;
                } else {
                    dz--;
                }
                base = Rail.Orientation.STRAIGHT_NORTH_SOUTH;
                break;
            default:
                return false;
        }

        return canPowered(Vector3i.from(dx, dy, dz), base, power, relative)
                || onStraight && canPowered(Vector3i.from(dx, dy - 1, dz), base, power, relative);
    }

    protected boolean canPowered(Vector3i pos, Rail.Orientation state, int power, boolean relative) {
        BlockState blockState = level.getBlock(pos);

        if (!(blockState instanceof BlockBehaviorRailActivator)) {
            return false;
        }

        Rail.Orientation base = ((BlockBehaviorRailActivator) blockState).getOrientation();

        return (state != Rail.Orientation.STRAIGHT_EAST_WEST
                || base != Rail.Orientation.STRAIGHT_NORTH_SOUTH
                && base != Rail.Orientation.ASCENDING_NORTH
                && base != Rail.Orientation.ASCENDING_SOUTH)
                && (state != Rail.Orientation.STRAIGHT_NORTH_SOUTH
                || base != Rail.Orientation.STRAIGHT_EAST_WEST
                && base != Rail.Orientation.ASCENDING_EAST
                && base != Rail.Orientation.ASCENDING_WEST)
                && (level.isBlockPowered(pos) || checkSurrounding(pos, relative, power + 1));
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(ACTIVATOR_RAIL)
        };
    }

}
