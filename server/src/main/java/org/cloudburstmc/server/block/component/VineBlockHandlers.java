package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.CloudBlockConnections;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.random.RandomGenerator;

@UtilityClass
public class VineBlockHandlers {

    private static final Direction[] DIRECTIONS = Direction.values();

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, placement, context) ->
            placement.getType() != BlockTypes.VINE || canAddFace(block, context.clickedFace().getOpposite());

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, _, face, _) -> {
        BlockState current = block.getState();
        int bits = faces(current);

        Direction support = face.getOpposite();
        if (support == Direction.UP && current.getType() != BlockTypes.VINE && hasSupport(block, support)) {
            return state.withTrait(BlockTraits.VINE_DIRECTION_BITS, 0);
        }

        if (canAddFace(block, support)) {
            return state.withTrait(BlockTraits.VINE_DIRECTION_BITS, bits | bit(support));
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (canAddFace(block, direction)) {
                return state.withTrait(BlockTraits.VINE_DIRECTION_BITS, bits | bit(direction));
            }
        }

        return null;
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> supportedFaces(block) != 0 || hasSupport(block, Direction.UP);

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, _) -> {
        BlockState state = block.getState();
        int supported = supportedFaces(block);

        if (supported == 0 && !hasSupport(block, Direction.UP)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        } else if (supported != faces(state)) {
            block.set(state.withTrait(BlockTraits.VINE_DIRECTION_BITS, supported));
        }
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        if (random.nextInt(4) != 0) {
            return;
        }

        Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        int faces = faces(block.getState());

        if (direction.getAxis().isHorizontal() && (faces & bit(direction)) == 0) {
            spreadSideways(block, direction, faces, random);
        } else if (direction != Direction.UP || !hasSupport(block, Direction.UP)) {
            if (direction == Direction.UP && isEmpty(block.getSide(Direction.UP))) {
                spreadUpwards(block, faces, random);
            } else {
                spreadDownwards(block, faces, random);
            }
        }
    };

    public static boolean isEmpty(Block block) {
        int y = block.getPosition().getY();
        Level level = block.getLevel();
        return y >= level.getMinHeight() && y < level.getMaxHeight()
                && block.getState().getType() == BlockTypes.AIR
                && block.getLiquid().isEmpty();
    }

    public static void spread(Block source, Block target, BlockState state) {
        if (source.getLevel().getLoadedBlock(target.getPosition()) != null) {
            applyGrowth(source, target, new BlockSpreadEvent(target, source, state));
        }
    }

    private static void spreadSideways(Block source, Direction direction, int faces, RandomGenerator random) {
        Block target = loadedNeighbor(source, direction);
        if (target == null || exceedsSpreadLimit(source)) {
            return;
        }

        if (!isEmpty(target)) {
            if (hasSupport(source, direction)) {
                applyGrowth(source, source, new BlockGrowEvent(source, vine(faces | bit(direction))));
            }

            return;
        }

        Direction clockwise = direction.rotateClockwise();
        Direction counter = direction.rotateCounterClockwise();

        for (Direction side : new Direction[]{clockwise, counter}) {
            if ((faces & bit(side)) != 0 && hasSupport(target, side)) {
                spread(source, target, vine(bit(side)));
                return;
            }
        }

        for (Direction side : new Direction[]{clockwise, counter}) {
            Block corner = loadedNeighbor(target, side);
            Direction back = direction.getOpposite();
            if ((faces & bit(side)) != 0 && corner != null && isEmpty(corner) && hasSupport(corner, back)) {
                spread(source, corner, vine(bit(back)));
                return;
            }
        }

        if (random.nextFloat() < 0.05f && hasSupport(target, Direction.UP)) {
            spread(source, target, vine(0));
        }
    }

    private static void spreadUpwards(Block source, int faces, RandomGenerator random) {
        if (exceedsSpreadLimit(source)) {
            return;
        }

        Block above = source.getSide(Direction.UP);
        int added = 0;

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if ((faces & bit(side)) != 0 && random.nextBoolean() && hasSupport(above, side)) {
                added |= bit(side);
            }
        }

        if (added != 0) {
            spread(source, above, vine(added));
        }
    }

    private static void spreadDownwards(Block source, int faces, RandomGenerator random) {
        Block below = source.getSide(Direction.DOWN);
        BlockState state = below.getState();

        if (!isEmpty(below) && state.getType() != BlockTypes.VINE) {
            return;
        }

        int previous = faces(state);
        int added = previous | (faces & random.nextInt(16));
        if (added != previous) {
            spread(source, below, vine(added));
        }
    }

    private static boolean exceedsSpreadLimit(Block source) {
        Vector3i position = source.getPosition();
        Level level = source.getLevel();

        int minX = position.getX() - 4;
        int maxX = position.getX() + 4;
        int minZ = position.getZ() - 4;
        int maxZ = position.getZ() + 4;
        int minY = Math.max(level.getMinHeight(), position.getY() - 1);
        int maxY = Math.min(level.getMaxHeight() - 1, position.getY() + 1);

        int count = 0;

        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                Chunk chunk = level.getLoadedChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                int startX = Math.max(minX, chunkX << 4);
                int endX = Math.min(maxX, (chunkX << 4) + 15);
                int startZ = Math.max(minZ, chunkZ << 4);
                int endZ = Math.min(maxZ, (chunkZ << 4) + 15);

                for (int x = startX; x <= endX; x++) {
                    for (int z = startZ; z <= endZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            if (chunk.getBlockState(x & 15, y, z & 15).getType() == BlockTypes.VINE && ++count > 4) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void applyGrowth(Block source, Block target, BlockGrowEvent event) {
        BlockState original = target.getState();
        BlockState parent = source.getState();
        target.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled()
                || source.getLevel().getBlockState(source.getPosition()) != parent
                || target.getLevel().getBlockState(target.getPosition()) != original) {
            return;
        }

        target.set(event.getNewState());
    }

    private static boolean canAddFace(Block block, Direction support) {
        return support.getAxis().isHorizontal() && (faces(block.getState()) & bit(support)) == 0
                && (hasSupport(block, support) || (inheritedFaces(block) & bit(support)) != 0);
    }

    private static int supportedFaces(Block block) {
        int current = faces(block.getState());
        int supported = current & inheritedFaces(block);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int bit = bit(direction);
            if ((current & bit) != 0 && hasSupport(block, direction)) {
                supported |= bit;
            }
        }

        return supported;
    }

    private static int inheritedFaces(Block block) {
        return faces(block.getSide(Direction.UP).getState());
    }

    private static boolean hasSupport(Block block, Direction direction) {
        Block support = loadedNeighbor(block, direction);
        if (support == null) {
            return false;
        }

        Direction face = direction.getOpposite();
        if (support.isFaceSturdy(face, SupportType.FULL)) {
            return true;
        }

        return CloudVoxelShapes.isFullBlock(support.getCollisionShape(CollisionContext.empty()).getFaceShape(face));
    }

    private static @Nullable Block loadedNeighbor(Block block, Direction direction) {
        return block.getLevel().getLoadedBlock(direction.relative(block.getPosition()));
    }

    private static int bit(Direction direction) {
        return CloudBlockConnections.VINE_BITS.getOrDefault(direction, 0);
    }

    private static int faces(BlockState state) {
        return state.getType() == BlockTypes.VINE ? state.ensureTrait(BlockTraits.VINE_DIRECTION_BITS) : 0;
    }

    private static BlockState vine(int faces) {
        return BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, faces);
    }
}
