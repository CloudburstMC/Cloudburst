package org.cloudburstmc.server.block.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.RailDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RailConnector {

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public static RailDirection getDirection(BlockState state) {
        if (state.getTraits().containsKey(BlockTraits.RAIL_DIRECTION)) {
            return (RailDirection) state.getTraits().get(BlockTraits.RAIL_DIRECTION);
        }
        if (state.getTraits().containsKey(BlockTraits.SIMPLE_RAIL_DIRECTION)) {
            return (RailDirection) state.getTraits().get(BlockTraits.SIMPLE_RAIL_DIRECTION);
        }
        return RailDirection.NORTH_SOUTH;
    }

    public static BlockState applyDirection(BlockState state, RailDirection direction) {
        if (direction.isCurved() && !canCurve(state)) {
            throw new IllegalStateException("Attempted to apply curved direction " + direction + " to straight-only block " + state.getType().getId());
        }

        if (state.getTraits().containsKey(BlockTraits.RAIL_DIRECTION)) {
            return state.withTrait(BlockTraits.RAIL_DIRECTION, direction);
        }

        if (state.getTraits().containsKey(BlockTraits.SIMPLE_RAIL_DIRECTION)) {
            return state.withTrait(BlockTraits.SIMPLE_RAIL_DIRECTION, direction);
        }

        return state;
    }

    public static boolean isRail(BlockState state) {
        return state.is(BlockTags.RAIL);
    }

    public static boolean canCurve(BlockState state) {
        return state.getType() == BlockTypes.RAIL;
    }

    public static boolean hasRigidSupport(CloudLevel level, Vector3i pos) {
        return BlockSupport.canSupportRigidBlock(level, pos.add(0, -1, 0));
    }

    public static void updateSurroundingRails(CloudLevel level, Vector3i pos) {
        for (Direction face : HORIZONTALS) {
            Vector3i sidePos = face.relative(pos);
            notifyIfRail(level, sidePos);
            notifyIfRail(level, sidePos.add(0, 1, 0));
            notifyIfRail(level, sidePos.add(0, -1, 0));
        }
    }

    public static BlockState place(CloudLevel level, Vector3i pos, BlockState state) {
        return updateDir(level, pos, state, false, true);
    }

    public static BlockState updateDir(CloudLevel level, Vector3i pos, BlockState state, boolean hasSignal) {
        return updateDir(level, pos, state, hasSignal, false);
    }

    private static BlockState updateDir(CloudLevel level, Vector3i pos, BlockState state, boolean hasSignal, boolean first) {
        RailDirection current = getDirection(state);
        RailStateCtx ctx = new RailStateCtx(level, pos, state, current);
        ctx.place(hasSignal, first, current);
        return ctx.state;
    }

    private static void notifyIfRail(CloudLevel level, Vector3i pos) {
        if (isRail(level.getBlockState(pos.getX(), pos.getY(), pos.getZ()))) {
            level.updateAround(pos);
        }
    }

    private static final class RailStateCtx {
        private final CloudLevel level;
        private final Vector3i pos;
        private final boolean isStraight;
        private final List<Vector3i> connections = new ArrayList<>();
        private BlockState state;

        RailStateCtx(CloudLevel level, Vector3i pos, BlockState state, RailDirection direction) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.isStraight = !canCurve(state);
            updateConnections(direction);
        }

        private void updateConnections(RailDirection direction) {
            connections.clear();
            switch (direction) {
                case NORTH_SOUTH -> {
                    connections.add(pos.add(0, 0, -1));
                    connections.add(pos.add(0, 0, 1));
                }
                case EAST_WEST -> {
                    connections.add(pos.add(-1, 0, 0));
                    connections.add(pos.add(1, 0, 0));
                }
                case ASCENDING_EAST -> {
                    connections.add(pos.add(-1, 0, 0));
                    connections.add(pos.add(1, 1, 0));
                }
                case ASCENDING_WEST -> {
                    connections.add(pos.add(-1, 1, 0));
                    connections.add(pos.add(1, 0, 0));
                }
                case ASCENDING_NORTH -> {
                    connections.add(pos.add(0, 1, -1));
                    connections.add(pos.add(0, 0, 1));
                }
                case ASCENDING_SOUTH -> {
                    connections.add(pos.add(0, 0, -1));
                    connections.add(pos.add(0, 1, 1));
                }
                case SOUTH_EAST -> {
                    connections.add(pos.add(1, 0, 0));
                    connections.add(pos.add(0, 0, 1));
                }
                case SOUTH_WEST -> {
                    connections.add(pos.add(-1, 0, 0));
                    connections.add(pos.add(0, 0, 1));
                }
                case NORTH_WEST -> {
                    connections.add(pos.add(-1, 0, 0));
                    connections.add(pos.add(0, 0, -1));
                }
                case NORTH_EAST -> {
                    connections.add(pos.add(1, 0, 0));
                    connections.add(pos.add(0, 0, -1));
                }
            }
        }

        private void removeSoftConnections() {
            for (int i = 0; i < connections.size(); i++) {
                RailStateCtx rail = getRail(connections.get(i));
                if (rail != null && rail.connectsTo(this)) {
                    connections.set(i, rail.pos);
                } else {
                    connections.remove(i--);
                }
            }
        }

        private boolean connectsTo(RailStateCtx other) {
            return hasConnection(other.pos);
        }

        private boolean hasConnection(Vector3i railPos) {
            for (Vector3i c : connections) {
                if (c.getX() == railPos.getX() && c.getZ() == railPos.getZ()) return true;
            }
            return false;
        }

        private boolean canConnectTo(RailStateCtx rail) {
            return connectsTo(rail) || connections.size() != 2;
        }

        private RailDirection promoteToAscending(RailDirection shape, Vector3i north, Vector3i south, Vector3i west, Vector3i east) {
            if (shape == RailDirection.NORTH_SOUTH) {
                if (isRail(level.getBlockState(north.getX(), north.getY() + 1, north.getZ()))) {
                    shape = RailDirection.ASCENDING_NORTH;
                }

                if (isRail(level.getBlockState(south.getX(), south.getY() + 1, south.getZ()))) {
                    shape = RailDirection.ASCENDING_SOUTH;
                }
            }

            if (shape == RailDirection.EAST_WEST) {
                if (isRail(level.getBlockState(east.getX(), east.getY() + 1, east.getZ()))) {
                    shape = RailDirection.ASCENDING_EAST;
                }

                if (isRail(level.getBlockState(west.getX(), west.getY() + 1, west.getZ()))) {
                    shape = RailDirection.ASCENDING_WEST;
                }
            }

            return shape;
        }

        private void connectTo(RailStateCtx rail) {
            connections.add(rail.pos);
            Vector3i north = pos.add(0, 0, -1);
            Vector3i south = pos.add(0, 0, 1);
            Vector3i west = pos.add(-1, 0, 0);
            Vector3i east = pos.add(1, 0, 0);

            boolean n = hasConnection(north);
            boolean s = hasConnection(south);
            boolean w = hasConnection(west);
            boolean e = hasConnection(east);

            RailDirection shape = null;
            if (n || s) {
                shape = RailDirection.NORTH_SOUTH;
            }

            if (w || e) {
                shape = RailDirection.EAST_WEST;
            }

            if (!isStraight) {
                if (s && e && !n && !w) {
                    shape = RailDirection.SOUTH_EAST;
                }

                if (s && w && !n && !e) {
                    shape = RailDirection.SOUTH_WEST;
                }

                if (n && w && !s && !e) {
                    shape = RailDirection.NORTH_WEST;
                }

                if (n && e && !s && !w) {
                    shape = RailDirection.NORTH_EAST;
                }
            }

            if (shape != null) {
                shape = promoteToAscending(shape, north, south, west, east);
            }

            if (shape == null) {
                shape = RailDirection.NORTH_SOUTH;
            }

            state = applyDirection(state, shape);
            level.setBlockState(pos, state, true, false);
        }

        private boolean hasNeighborRail(Vector3i neighborPos) {
            RailStateCtx neighbor = getRail(neighborPos);
            if (neighbor == null) {
                return false;
            }

            neighbor.removeSoftConnections();
            return neighbor.canConnectTo(this);
        }

        void place(boolean hasSignal, boolean first, RailDirection defaultShape) {
            Vector3i north = pos.add(0, 0, -1);
            Vector3i south = pos.add(0, 0, 1);
            Vector3i west = pos.add(-1, 0, 0);
            Vector3i east = pos.add(1, 0, 0);

            boolean n = hasNeighborRail(north);
            boolean s = hasNeighborRail(south);
            boolean w = hasNeighborRail(west);
            boolean e = hasNeighborRail(east);

            RailDirection shape = null;
            boolean northOrSouth = n || s;
            boolean westOrEast = w || e;

            if (northOrSouth && !westOrEast) {
                shape = RailDirection.NORTH_SOUTH;
            }

            if (westOrEast && !northOrSouth) {
                shape = RailDirection.EAST_WEST;
            }

            boolean se = s && e;
            boolean sw = s && w;
            boolean ne = n && e;
            boolean nw = n && w;

            if (!isStraight) {
                if (se && !n && !w) {
                    shape = RailDirection.SOUTH_EAST;
                }

                if (sw && !n && !e) {
                    shape = RailDirection.SOUTH_WEST;
                }

                if (nw && !s && !e) {
                    shape = RailDirection.NORTH_WEST;
                }

                if (ne && !s && !w) {
                    shape = RailDirection.NORTH_EAST;
                }
            }

            if (shape == null) {
                if (northOrSouth && westOrEast) {
                    shape = defaultShape;
                } else if (northOrSouth) {
                    shape = RailDirection.NORTH_SOUTH;
                } else if (westOrEast) {
                    shape = RailDirection.EAST_WEST;
                }

                if (!isStraight) {
                    if (hasSignal) {
                        if (se) {
                            shape = RailDirection.SOUTH_EAST;
                        }

                        if (sw) {
                            shape = RailDirection.SOUTH_WEST;
                        }

                        if (ne) {
                            shape = RailDirection.NORTH_EAST;
                        }

                        if (nw) {
                            shape = RailDirection.NORTH_WEST;
                        }
                    } else {
                        if (nw) {
                            shape = RailDirection.NORTH_WEST;
                        }

                        if (ne) {
                            shape = RailDirection.NORTH_EAST;
                        }

                        if (sw) {
                            shape = RailDirection.SOUTH_WEST;
                        }

                        if (se) {
                            shape = RailDirection.SOUTH_EAST;
                        }
                    }
                }
            }

            shape = promoteToAscending(shape != null ? shape : defaultShape, north, south, west, east);
            if (shape == null) {
                shape = defaultShape;
            }

            updateConnections(shape);
            state = applyDirection(state, shape);

            BlockState worldState = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            if (first || worldState != state) {
                level.setBlockState(pos, state, true, false);

                for (Vector3i connPos : connections) {
                    RailStateCtx neighbor = getRail(connPos);
                    if (neighbor != null) {
                        neighbor.removeSoftConnections();
                        if (neighbor.canConnectTo(this)) {
                            neighbor.connectTo(this);
                        }
                    }
                }
            }
        }

        private RailStateCtx getRail(Vector3i targetPos) {
            BlockState state = level.getBlockState(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            if (isRail(state)) {
                return new RailStateCtx(level, targetPos, state, getDirection(state));
            }

            Vector3i up = targetPos.add(0, 1, 0);
            state = level.getBlockState(up.getX(), up.getY(), up.getZ());
            if (isRail(state)) {
                return new RailStateCtx(level, up, state, getDirection(state));
            }

            Vector3i down = targetPos.add(0, -1, 0);
            state = level.getBlockState(down.getX(), down.getY(), down.getZ());
            if (isRail(state)) {
                return new RailStateCtx(level, down, state, getDirection(state));
            }

            return null;
        }
    }
}
