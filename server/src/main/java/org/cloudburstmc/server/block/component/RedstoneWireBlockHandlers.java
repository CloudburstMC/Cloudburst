package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.RedstoneSignalHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RedstoneSide;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.RedstoneSignals;

@UtilityClass
public class RedstoneWireBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> resolveConnections(block.getLevel(), block.getPosition(), withAllConnections(state, RedstoneSide.SIDE));

    public static final SurviveBlockHandler CAN_SURVIVE = block -> canSurviveOn(block.getLevel(), Direction.DOWN.relative(block.getPosition()));

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> update(block, directionTo(block.getPosition(), neighbor.getPosition()));

    public static final ComplexBlockHandler ON_REDSTONE_UPDATE = block -> update(block, null);

    public static final ComplexBlockHandler ON_ADDED = block -> {
        update(block, null);
        notifyNeighbors(block.getLevel(), block.getPosition());
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> notifyNeighbors(block.getLevel(), block.getPosition());

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) -> {
        if (face == Direction.UP) {
            return 0;
        }

        int power = block.getState().ensureTrait(BlockTraits.REDSTONE_SIGNAL);
        if (power == 0 || face == Direction.DOWN) {
            return power;
        }

        BlockState connected = resolveConnections(block.getLevel(), block.getPosition(), block.getState());
        return isConnected(connected, face) ? power : 0;
    };

    public static final RedstoneSignalHandler GET_DIRECT_REDSTONE_SIGNAL = GET_REDSTONE_SIGNAL;

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        if (!player.getAbilities().get(Ability.BUILD)) {
            return false;
        }

        Block current = block.refresh();
        BlockState state = current.getState();
        if (state.getType() != BlockTypes.REDSTONE_WIRE || !isCross(state) && !isDot(state)) {
            return false;
        }

        BlockState proposed = withAllConnections(state, isCross(state) ? RedstoneSide.NONE : RedstoneSide.SIDE);
        BlockState resolved = resolveConnections(current.getLevel(), current.getPosition(), proposed);
        if (resolved == state) {
            return false;
        }

        current.getLevel().setBlockState(current.getPosition(), resolved, false, false);
        notifyNeighbors(current.getLevel(), current.getPosition());
        return true;
    };

    public static BlockState resolveConnections(Level level, Vector3i position, BlockState state) {
        boolean wasDot = isDot(state);
        BlockState resolved = withAllConnections(state, RedstoneSide.NONE);
        boolean canConnectUp = !conducts(level, Direction.UP.relative(position));

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            resolved = resolved.withTrait(connectionTrait(direction), connectingSide(level, position, direction, canConnectUp));
        }

        if (wasDot && isDot(resolved)) {
            return resolved;
        }

        boolean northSouthEmpty = !isConnected(resolved, Direction.NORTH) && !isConnected(resolved, Direction.SOUTH);
        boolean eastWestEmpty = !isConnected(resolved, Direction.EAST) && !isConnected(resolved, Direction.WEST);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            boolean perpendicularEmpty = direction.getAxis() == Direction.Axis.X ? northSouthEmpty : eastWestEmpty;
            if (!isConnected(resolved, direction) && perpendicularEmpty) {
                resolved = resolved.withTrait(connectionTrait(direction), RedstoneSide.SIDE);
            }
        }

        return resolved;
    }

    public static int calculateTargetStrength(Block block) {
        int external = RedstoneSignals.getExternalSignal(block);
        if (external == 15) {
            return external;
        }

        Level level = block.getLevel();
        Vector3i position = block.getPosition();
        boolean canConnectUp = !conducts(level, Direction.UP.relative(position));
        int incoming = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i neighbor = direction.relative(position);
            incoming = Math.max(incoming, wirePower(level, neighbor));

            if (conducts(level, neighbor)) {
                if (canConnectUp) {
                    incoming = Math.max(incoming, wirePower(level, Direction.UP.relative(neighbor)));
                }
            } else {
                incoming = Math.max(incoming, wirePower(level, Direction.DOWN.relative(neighbor)));
            }
        }

        return Math.max(external, Math.max(0, incoming - 1));
    }

    public static EnumBlockTrait<RedstoneSide> connectionTrait(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockTraits.REDSTONE_NORTH;
            case EAST -> BlockTraits.REDSTONE_EAST;
            case SOUTH -> BlockTraits.REDSTONE_SOUTH;
            case WEST -> BlockTraits.REDSTONE_WEST;
            default -> throw new IllegalArgumentException("Wire connections require a horizontal direction: " + direction);
        };
    }

    private static void update(Block block, @Nullable Direction changedDirection) {
        Block current = block.refresh();
        BlockState state = current.getState();
        if (state.getType() != BlockTypes.REDSTONE_WIRE) {
            return;
        }

        Level level = current.getLevel();
        Vector3i position = current.getPosition();
        if (!CAN_SURVIVE.execute(current)) {
            level.breakBlock(position, ItemStack.EMPTY, null, true);
            return;
        }

        BlockState resolved = state;
        if (changedDirection == Direction.UP) {
            resolved = resolveConnections(level, position, state);
        } else if (changedDirection != null && changedDirection != Direction.DOWN) {
            RedstoneSide side = connectingSide(level, position, changedDirection, !conducts(level, Direction.UP.relative(position)));
            resolved = (side != RedstoneSide.NONE) == isConnected(state, changedDirection) && !isCross(state)
                    ? state.withTrait(connectionTrait(changedDirection), side)
                    : resolveConnections(level, position, withAllConnections(state, RedstoneSide.SIDE));
        }

        resolved = resolved.withTrait(BlockTraits.REDSTONE_SIGNAL, calculateTargetStrength(current));
        if (resolved != state && level.getBlockState(position) == state) {
            level.setBlockState(position, resolved, false, false);
            notifyNeighbors(level, position);
        }
    }

    private static RedstoneSide connectingSide(Level level, Vector3i position, Direction direction, boolean canConnectUp) {
        Vector3i neighborPosition = direction.relative(position);
        BlockState neighbor = level.getBlockState(neighborPosition);
        if (canConnectUp && (neighbor.is(BlockTags.TRAPDOOR) || canSurviveOn(level, neighborPosition)) && connectsTo(level, Direction.UP.relative(neighborPosition), null)) {
            return BlockSupport.isFaceSturdy(level, neighborPosition, direction.getOpposite()) ? RedstoneSide.UP : RedstoneSide.SIDE;
        }

        if (connectsTo(level, neighborPosition, direction)) {
            return RedstoneSide.SIDE;
        }

        return !conducts(level, neighborPosition) && connectsTo(level, Direction.DOWN.relative(neighborPosition), null) ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    private static boolean connectsTo(Level level, Vector3i position, @Nullable Direction direction) {
        Block block = level.getBlock(position);
        return block.requireComponent(BlockComponents.CONNECTS_TO_REDSTONE_WIRE).execute(block, direction);
    }

    private static boolean canSurviveOn(Level level, Vector3i position) {
        return level.getBlockState(position).getType() == BlockTypes.HOPPER || BlockSupport.isFaceSturdy(level, position, Direction.UP);
    }

    private static boolean conducts(Level level, Vector3i position) {
        Block block = level.getLoadedBlock(position);
        return block != null && block.requireComponent(BlockComponents.CONDUCTS_REDSTONE).execute(block.getState());
    }

    private static int wirePower(Level level, Vector3i position) {
        Block block = level.getLoadedBlock(position);
        return block != null && block.getState().getType() == BlockTypes.REDSTONE_WIRE ? block.getState().ensureTrait(BlockTraits.REDSTONE_SIGNAL) : 0;
    }

    private static BlockState withAllConnections(BlockState state, RedstoneSide side) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.withTrait(connectionTrait(direction), side);
        }

        return state;
    }

    private static boolean isConnected(BlockState state, Direction direction) {
        return state.ensureTrait(connectionTrait(direction)) != RedstoneSide.NONE;
    }

    private static boolean isDot(BlockState state) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isConnected(state, direction)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isCross(BlockState state) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!isConnected(state, direction)) {
                return false;
            }
        }

        return true;
    }

    private static @Nullable Direction directionTo(Vector3i position, Vector3i neighborPosition) {
        for (Direction direction : Direction.values()) {
            if (direction.relative(position).equals(neighborPosition)) {
                return direction;
            }
        }

        return null;
    }

    private static void notifyNeighbors(Level level, Vector3i position) {
        level.updateAround(position);

        for (Direction direction : Direction.values()) {
            level.updateAround(direction.relative(position));
        }
    }
}
