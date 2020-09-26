package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.Rail;
import org.cloudburstmc.server.utils.data.RailDirection;

import static org.cloudburstmc.server.block.BlockTypes.GOLDEN_RAIL;

public class BlockBehaviorRailPowered extends BlockBehaviorRail {

    public BlockBehaviorRailPowered() {
        super(GOLDEN_RAIL, BlockTraits.SIMPLE_RAIL_DIRECTION);
        canBePowered = true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        // Warning: I din't recommended this on slow networks server or slow client
        //          Network below 86Kb/s. This will became unresponsive to clients 
        //          When updating the block state. Espicially on the world with many rails. 
        //          Trust me, I tested this on my server.
        if (type == Level.BLOCK_UPDATE_NORMAL || type == Level.BLOCK_UPDATE_REDSTONE || type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (super.onUpdate(block, type) == Level.BLOCK_UPDATE_NORMAL) {
                return 0; // Already broken
            }
            boolean wasPowered = isActive(block.getState());
            boolean isPowered = block.getLevel().isBlockPowered(block.getPosition())
                    || checkSurrounding(block, block.getPosition(), true, 0)
                    || checkSurrounding(block, block.getPosition(), false, 0);

            // Avoid Block minstake
            if (wasPowered != isPowered) {
                setActive(block, isPowered);
                block.getLevel().updateAround(block.getPosition().down());
                if (getOrientation(block.getState()).isAscending()) {
                    block.getLevel().updateAround(block.getPosition().up());
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
    protected boolean checkSurrounding(Block block, Vector3i pos, boolean relative, int power) {
        // The powered rail can power up to 8 blocks only
        if (power >= 8) {
            return false;
        }
        // The position of the floor numbers
        int dx = pos.getX();
        int dy = pos.getY();
        int dz = pos.getZ();
        // First: get the base block
        BlockBehaviorRail behavior;
        BlockState state = block.getLevel().getBlock(dx, dy, dz).getState();

        // Second: check if the rail is Powered rail
        if (Rail.isRailBlock(state)) {
            behavior = (BlockBehaviorRail) state.getBehavior();
        } else {
            return false;
        }

        // Used to check if the next ascending rail should be what
        RailDirection base = null;
        boolean onStraight = true;
        // Third: Recalculate the base position
        switch (behavior.getOrientation(state)) {
            case NORTH_SOUTH:
                if (relative) {
                    dz++;
                } else {
                    dz--;
                }
                break;
            case EAST_WEST:
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
                base = RailDirection.EAST_WEST;
                break;
            case ASCENDING_WEST:
                if (relative) {
                    dx--;
                    dy++;
                    onStraight = false;
                } else {
                    dx++;
                }
                base = RailDirection.EAST_WEST;
                break;
            case ASCENDING_NORTH:
                if (relative) {
                    dz++;
                } else {
                    dz--;
                    dy++;
                    onStraight = false;
                }
                base = RailDirection.NORTH_SOUTH;
                break;
            case ASCENDING_SOUTH:
                if (relative) {
                    dz++;
                    dy++;
                    onStraight = false;
                } else {
                    dz--;
                }
                base = RailDirection.NORTH_SOUTH;
                break;
            default:
                // Unable to determinate the rail orientation
                // Wrong rail?
                return false;
        }
        // Next check the if rail is on power state
        return canPowered(block, Vector3i.from(dx, dy, dz), base, power, relative)
                || onStraight && canPowered(block, Vector3i.from(dx, dy - 1, dz), base, power, relative);
    }

    protected boolean canPowered(Block block, Vector3i pos, RailDirection direction, int power, boolean relative) {
        BlockState state = block.getLevel().getBlock(pos).getState();
        // What! My block is air??!! Impossible! XD
        if (state.getType() != GOLDEN_RAIL) {
            return false;
        }

        // Sometimes the rails are diffrent orientation
        RailDirection base = ((BlockBehaviorRailPowered) state.getBehavior()).getOrientation(state);

        // Possible way how to know when the rail is activated is rail were directly powered
        // OR recheck the surrounding... Which will returns here =w=        
        return (direction != RailDirection.EAST_WEST
                || base != RailDirection.NORTH_SOUTH
                && base != RailDirection.ASCENDING_NORTH
                && base != RailDirection.ASCENDING_SOUTH)
                && (direction != RailDirection.NORTH_SOUTH
                || base != RailDirection.EAST_WEST
                && base != RailDirection.ASCENDING_EAST
                && base != RailDirection.ASCENDING_WEST)
                && (block.getLevel().isBlockPowered(pos) || checkSurrounding(block, pos, relative, power + 1));
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(GOLDEN_RAIL)
        };
    }
}
