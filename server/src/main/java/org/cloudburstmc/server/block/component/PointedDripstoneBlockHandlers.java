package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.event.block.BlockFormEvent;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.level.particle.ParticleEmission;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.server.block.util.BlockShapeTransitions;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static org.cloudburstmc.server.block.component.SpeleothemBlockHandlers.hanging;
import static org.cloudburstmc.server.block.component.SpeleothemBlockHandlers.loadedVertical;

@UtilityClass
public class PointedDripstoneBlockHandlers {

    private static final float WATER_TRANSFER_CHANCE = 0.17578125f;
    private static final float LAVA_TRANSFER_CHANCE = 0.05859375f;
    private static final float GROWTH_CHANCE = 0.011377778f;
    private static final int DRIP_SEARCH_LIMIT = 11;
    private static final int GROWTH_SEARCH_LIMIT = 7;
    private static final BoundingBox DRIP_SPACE = new BoundingBox(6 / 16f, 0, 6 / 16f, 10 / 16f, 1, 10 / 16f);

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        RandomGenerator source = random == null ? ThreadLocalRandom.current() : random;
        transferFluid(block, source.nextFloat());
        if (source.nextFloat() < GROWTH_CHANCE) {
            grow(block, source);
        }
    };

    public static @Nullable Block findTipAbove(Block cauldron) {
        for (int distance = 1; distance < DRIP_SEARCH_LIMIT; distance++) {
            Block candidate = loadedVertical(cauldron, distance);
            if (candidate == null) {
                return null;
            }

            if (isFreeTip(candidate)) {
                return candidate;
            }

            if (blocksDripping(candidate)) {
                return null;
            }
        }

        return null;
    }

    public static @Nullable LiquidType fillingFluid(Block tip) {
        FluidSource source = fluidSource(tip);
        return source == null ? null : source.fluid();
    }

    public static void transferFluid(Block start, float roll) {
        if (roll >= WATER_TRANSFER_CHANCE || isInvalidStalactiteStart(start)) {
            return;
        }

        FluidSource source = fluidSource(start);
        if (source == null) {
            return;
        }

        if (roll >= (source.fluid() == LiquidTypes.WATER ? WATER_TRANSFER_CHANCE : LAVA_TRANSFER_CHANCE)) {
            return;
        }

        Block tip = findTip(start, DRIP_SEARCH_LIMIT);
        if (tip == null) {
            return;
        }

        if (source.block().getState().getType() == BlockTypes.MUD) {
            Block mud = source.block();
            BlockFormEvent event = new BlockFormEvent(mud, BlockTypes.CLAY.getDefaultState());
            mud.getLevel().getServer().getEventManager().fire(event);
            if (!event.isCancelled() && mud.getLevel().getBlockState(mud.getPosition()) == mud.getState() && mud.getLevel().setBlockState(mud.getPosition(), event.getNewState())) {
                if (mud.getLevel() instanceof CloudLevel level) {
                    BlockShapeTransitions.pushEntitiesUp(level, mud.getPosition(), mud.getState(), event.getNewState());
                }

                drip(tip, source.fluid());
            }

            return;
        }

        for (int distance = 1; distance < DRIP_SEARCH_LIMIT; distance++) {
            Block below = loadedVertical(tip, -distance);
            if (below == null) {
                return;
            }

            if (CauldronBlockHandlers.canReceiveDrip(below, source.fluid())) {
                below.getLevel().scheduleUpdate(below.getPosition(), 50 + distance);
                drip(tip, source.fluid());
                return;
            }

            if (blocksDripping(below)) {
                return;
            }
        }
    }

    public static void grow(Block start, RandomGenerator random) {
        if (isInvalidStalactiteStart(start)) {
            return;
        }

        Block root = loadedVertical(start, 1);
        Block water = loadedVertical(start, 2);
        if (root == null || water == null || root.getState().getType() != BlockTypes.DRIPSTONE_BLOCK
                || !water.getLiquid().isSource() || !water.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            return;
        }

        Block tip = findTip(start, GROWTH_SEARCH_LIMIT);
        if (tip == null || !isFreeTip(tip) || !canTipGrow(tip)) {
            return;
        }

        if (random.nextBoolean()) {
            extend(tip);
            return;
        }

        for (int distance = 1; distance <= 10; distance++) {
            Block target = loadedVertical(tip, -distance);
            if (target == null || !target.getLiquid().isEmpty()) {
                return;
            }

            if (isPointed(target.getState(), false) && isTip(target.getState(), false) && canTipGrow(target)) {
                extend(target);
                return;
            }

            if (target.getState() == BlockStates.AIR && SpeleothemBlockHandlers.canSurvive(target, BlockTypes.POINTED_DRIPSTONE, false)) {
                Block below = loadedVertical(target, -1);
                if (below != null && !below.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
                    spread(tip, target, pointed(false, DripstoneThickness.TIP));
                    return;
                }
            }

            if (blocksDripping(target)) {
                return;
            }
        }
    }

    private static @Nullable Block findTip(Block start, int limit) {
        boolean hanging = hanging(start.getState());
        Block current = start;
        for (int step = 0; step < limit; step++) {
            if (current == null || !isPointed(current.getState(), hanging)) {
                return null;
            }

            if (isTip(current.getState(), false)) {
                return current;
            }

            current = loadedVertical(current, hanging ? -1 : 1);
        }

        return null;
    }

    private static @Nullable FluidSource fluidSource(Block start) {
        if (!isPointed(start.getState(), true)) {
            return null;
        }

        for (int distance = 1; distance < DRIP_SEARCH_LIMIT; distance++) {
            Block root = loadedVertical(start, distance);
            if (root == null) {
                return null;
            }

            if (isPointed(root.getState(), true)) {
                continue;
            }

            if (root.getState().getType() == BlockTypes.POINTED_DRIPSTONE) {
                return null;
            }

            Block source = loadedVertical(root, 1);
            if (source == null) {
                return null;
            }

            if (source.getState().getType() == BlockTypes.MUD
                    && (!(source.getLevel() instanceof CloudLevel level) || level.getDimension() != CloudLevel.DIMENSION_NETHER)) {
                return new FluidSource(source, LiquidTypes.WATER);
            }

            LiquidState liquid = source.getLiquid();
            if (liquid.isSource() && liquid.getType().isSameFamily(LiquidTypes.WATER)) {
                return new FluidSource(source, LiquidTypes.WATER);
            }

            if (liquid.isSource() && liquid.getType().isSameFamily(LiquidTypes.LAVA)) {
                return new FluidSource(source, LiquidTypes.LAVA);
            }

            return null;
        }

        return null;
    }

    private static boolean blocksDripping(Block block) {
        return !block.getLiquid().isEmpty() || block.getCollisionShape(CollisionContext.empty()).overlaps(DRIP_SPACE);
    }

    private static boolean isInvalidStalactiteStart(Block block) {
        Block above = loadedVertical(block, 1);
        return !isPointed(block.getState(), true) || above == null || above.getState().getType() == BlockTypes.POINTED_DRIPSTONE;
    }

    private static boolean isFreeTip(Block block) {
        return isPointed(block.getState(), true) && isTip(block.getState(), false) && block.getLiquid().isEmpty();
    }

    private static boolean canTipGrow(Block tip) {
        boolean hanging = hanging(tip.getState());
        Block next = loadedVertical(tip, hanging ? -1 : 1);
        if (next == null || !next.getLiquid().isEmpty()) {
            return false;
        }

        BlockState state = next.getState();
        return state == BlockStates.AIR || isPointed(state, !hanging) && isTip(state, false);
    }

    private static void extend(Block tip) {
        boolean hanging = hanging(tip.getState());
        Block next = loadedVertical(tip, hanging ? -1 : 1);
        if (next == null) {
            return;
        }

        if (isPointed(next.getState(), !hanging) && isTip(next.getState(), false)) {
            if (spread(tip, next, pointed(!hanging, DripstoneThickness.MERGE))) {
                Block refreshed = tip.refresh();
                if (isPointed(refreshed.getState(), hanging) && isTip(refreshed.getState(), true)) {
                    spread(next, refreshed, pointed(hanging, DripstoneThickness.MERGE));
                }
            }
        } else if (next.getState() == BlockStates.AIR) {
            spread(tip, next, pointed(hanging, DripstoneThickness.TIP));
        }
    }

    private static boolean spread(Block source, Block target, BlockState state) {
        BlockSpreadEvent event = new BlockSpreadEvent(target, source, state);
        target.getLevel().getServer().getEventManager().fire(event);
        return !event.isCancelled() && target.getLevel().getBlockState(target.getPosition()) == target.getState()
                && target.getLevel().setBlockState(target.getPosition(), event.getNewState());
    }

    private static void drip(Block tip, LiquidType fluid) {
        BoundingBox bounds = tip.getOutlineShape().bounds();
        tip.getLevel().spawnParticle(
                new ParticleEmission(fluid == LiquidTypes.LAVA ? ParticleTypes.STALACTITE_DRIP_LAVA : ParticleTypes.STALACTITE_DRIP_WATER),
                tip.getPosition().toFloat().add((bounds.getMinX() + bounds.getMaxX()) * 0.5f,
                        bounds.getMinY() - 1 / 16f, (bounds.getMinZ() + bounds.getMaxZ()) * 0.5f));
    }

    private static boolean isPointed(BlockState state, boolean hanging) {
        return state.getType() == BlockTypes.POINTED_DRIPSTONE && hanging(state) == hanging;
    }

    private static boolean isTip(BlockState state, boolean merged) {
        DripstoneThickness thickness = state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS);
        return thickness == DripstoneThickness.TIP || merged && thickness == DripstoneThickness.MERGE;
    }

    private static BlockState pointed(boolean hanging, DripstoneThickness thickness) {
        return BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.IS_HANGING, hanging)
                .withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness);
    }

    private record FluidSource(Block block, LiquidType fluid) {
    }
}
