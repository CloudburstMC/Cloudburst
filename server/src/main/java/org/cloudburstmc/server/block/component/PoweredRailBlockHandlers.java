package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RailDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.RailConnector;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class PoweredRailBlockHandlers {

    private static final int MAX_CHAIN_LENGTH = 8;

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = block.getState();

        if (RailBlockHandlers.checkAndBreakIfUnsupported(block)) {
            return;
        }

        boolean currentlyPowered = Boolean.TRUE.equals(state.getTraits().get(BlockTraits.IS_POWERED));
        boolean shouldBePowered = checkPowered(block);

        if (currentlyPowered != shouldBePowered) {
            BlockState newState = state.withTrait(BlockTraits.IS_POWERED, shouldBePowered);
            level.setBlockState(pos, newState, true, false);

            level.updateAround(pos);

            RailDirection orientation = RailConnector.getDirection(state);
            if (orientation.isAscending()) {
                Direction ascendFace = orientation.ascendingDirection();
                if (ascendFace != null) {
                    level.updateAround(ascendFace.relative(pos));
                }
            }
        }
    };

    public static boolean checkPowered(Block block) {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = block.getState();

        if (isReceivingDirectPower(level, pos)) {
            return true;
        }

        RailDirection orientation = RailConnector.getDirection(state);
        BlockType myType = state.getType();

        for (Direction face : orientation.connectingDirections()) {
            if (checkChainPower(level, pos, face, orientation, myType, 0)) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkChainPower(CloudLevel level, Vector3i pos, Direction direction, RailDirection orientation, BlockType myType, int depth) {
        if (depth >= MAX_CHAIN_LENGTH) {
            return false;
        }

        Vector3i nextPos = direction.relative(pos);
        if (orientation.isAscending() && orientation.ascendingDirection() == direction) {
            nextPos = nextPos.add(0, 1, 0);
        }

        BlockState nextState = level.getBlockState(nextPos.getX(), nextPos.getY(), nextPos.getZ());
        if (nextState.getType() != myType) {
            Vector3i downPos = nextPos.add(0, -1, 0);
            BlockState downState = level.getBlockState(downPos.getX(), downPos.getY(), downPos.getZ());
            if (downState.getType() == myType) {
                nextPos = downPos;
                nextState = downState;
            } else {
                return false;
            }
        }

        RailDirection nextOrientation = RailConnector.getDirection(nextState);
        if (!nextOrientation.connectingDirections().contains(direction) && !nextOrientation.connectingDirections().contains(direction.getOpposite())) {
            return false;
        }

        if (isReceivingDirectPower(level, nextPos)) {
            return true;
        }

        return checkChainPower(level, nextPos, direction, nextOrientation, myType, depth + 1);
    }

    /**
     * TODO: Restone api/impl
     */
    static boolean isReceivingDirectPower(CloudLevel level, Vector3i pos) {
        return false;
    }
}

