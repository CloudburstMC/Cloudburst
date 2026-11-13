package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockPlacementTarget;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class ScaffoldingBlockHandlers {

    private static final int UNSUPPORTED = 7;
    private static final VoxelShape TOP = CloudVoxelShapes.box(0, 0.875f, 0, 1, 1, 1);
    private static final ComplexBlockHandler START_FALLING = FallingBlockHandlers.startFalling(
            SoundTypes.BLOCK_SCAFFOLDING_FALL, SoundTypes.BLOCK_SCAFFOLDING_BREAK, 0, 0);

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE =
            (state, block, player, face, click) -> withDistance(state, distance(block));

    public static final SurviveBlockHandler CAN_SURVIVE = block -> distance(block) < UNSUPPORTED;

    public static final ComplexBlockHandler ON_ADDED = FallingBlockHandlers.scheduleFall(1);

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> ON_ADDED.execute(block);

    public static final CollisionShapeHandler GET_COLLISION_SHAPE = new CollisionShapeHandler() {
        @Override
        public VoxelShape execute(BlockState state, BlockShapeContext context, CollisionContext entity) {
            if (entity.isPlacement() || context.isEmpty() || !entity.hasEntity()) {
                return CloudVoxelShapes.empty();
            }

            Vector3i position = context.position();
            boolean descending = entity.getEntity() instanceof CloudPlayer player && player.isDescendingScaffolding();
            return !descending && entity.isAbove(CloudVoxelShapes.block(), position, false) ? TOP : CloudVoxelShapes.empty();
        }

        @Override
        public ShapeContextRequirement contextRequirement() {
            return ShapeContextRequirement.ENTITY;
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        BlockState state = block.getState();
        int distance = distance(block);

        if (distance < UNSUPPORTED) {
            BlockState updated = withDistance(state, distance);
            if (updated != state) {
                block.set(updated);
            }

            return;
        }

        BlockState secondary = block.getSecondaryState();
        BlockState replacement = secondary.getType().isLiquid() ? secondary : BlockStates.AIR;
        BlockFadeEvent event = new BlockFadeEvent(block, replacement);
        Level level = block.getLevel();
        Vector3i position = block.getPosition();
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(position) != state) {
            return;
        }

        if (event.getNewState() != replacement) {
            block.set(event.getNewState());
        } else if (state.ensureTrait(BlockTraits.STABILITY) == UNSUPPORTED) {
            START_FALLING.execute(block);
        } else {
            level.breakBlock(position, ItemStack.EMPTY, null, true);
        }
    };

    public static @Nullable BlockPlacementTarget resolvePlacement(BlockState state, Block adjacent, BlockPlacementContext context) {
        Block clicked = context.clickedBlock();
        Direction face = context.clickedFace();
        boolean clickedScaffold = isScaffolding(clicked);

        if (clickedScaffold && face == Direction.DOWN) {
            return extendColumn(state, clicked, context);
        }

        Block target = !clickedScaffold && canReplace(clicked, state, context) ? clicked : adjacent;
        if (isScaffolding(target)) {
            return extendColumn(state, target, context);
        }

        if (!canReplace(target, state, context) || (!clickedScaffold && !CAN_SURVIVE.execute(target))) {
            return null;
        }

        return new BlockPlacementTarget(target, face);
    }

    private static @Nullable BlockPlacementTarget extendColumn(BlockState state, Block scaffold, BlockPlacementContext context) {
        Level level = scaffold.getLevel();
        Vector3i position = Direction.UP.relative(scaffold.getPosition());

        while (position.getY() < level.getMaxHeight()) {
            Block target = level.getLoadedBlock(position);
            if (target == null) {
                return null;
            }

            if (!isScaffolding(target)) {
                return canReplace(target, state, context) ? new BlockPlacementTarget(target, Direction.UP) : null;
            }

            position = Direction.UP.relative(position);
        }

        return null;
    }

    private static boolean canReplace(Block block, BlockState state, BlockPlacementContext context) {
        int y = block.getPosition().getY();
        Level level = block.getLevel();

        return y >= level.getMinHeight() && y < level.getMaxHeight()
                && !block.getLiquid().getType().isSameFamily(LiquidTypes.LAVA)
                && block.requireComponent(BlockComponents.CAN_BE_REPLACED).execute(block, state, context);
    }

    private static boolean isScaffolding(Block block) {
        return block.getState().getType() == BlockTypes.SCAFFOLDING;
    }

    private static int distance(Block block) {
        Block below = block.getSide(Direction.DOWN);
        BlockState support = below.getState();
        int distance = UNSUPPORTED;

        if (support.getType() == BlockTypes.SCAFFOLDING) {
            distance = support.ensureTrait(BlockTraits.STABILITY);
        } else if (below.isFaceSturdy(Direction.UP, SupportType.FULL)) {
            return 0;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = block.getSide(direction).getState();
            if (neighbor.getType() == BlockTypes.SCAFFOLDING) {
                distance = Math.min(distance, neighbor.ensureTrait(BlockTraits.STABILITY) + 1);
            }
        }

        return distance;
    }

    private static BlockState withDistance(BlockState state, int distance) {
        return state.withTrait(BlockTraits.STABILITY, distance)
                .withTrait(BlockTraits.HAS_STABILITY_CHECK, distance == UNSUPPORTED);
    }
}
