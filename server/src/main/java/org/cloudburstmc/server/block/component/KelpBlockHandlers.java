package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class KelpBlockHandlers {

    private static final int MAX_AGE = BlockTraits.KELP_AGE.getRange().getEnd();
    private static final double GROWTH_CHANCE = 0.14;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        LiquidState liquid = block.getLiquid();
        if (!liquid.getType().isSameFamily(LiquidTypes.WATER) || liquid.getAmount() != 8) {
            return null;
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        int age = above != null && above.getState().getType() == BlockTypes.KELP ? MAX_AGE : ThreadLocalRandom.current().nextInt(MAX_AGE);
        return state.withTrait(BlockTraits.KELP_AGE, age);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (!block.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            return false;
        }

        Vector3i below = block.getPosition().add(0, -1, 0);
        BlockState support = block.getLevel().getBlockState(below);
        return !support.is(BlockTags.CANNOT_SUPPORT_KELP) && (support.getType() == BlockTypes.KELP || BlockSupport.isFaceSturdy(block.getLevel(), below, Direction.UP));
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), 1);
            return;
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        if (above != null && above.getState().getType() == BlockTypes.KELP && block.getState().ensureTrait(BlockTraits.KELP_AGE) != MAX_AGE) {
            block.set(block.getState().withTrait(BlockTraits.KELP_AGE, MAX_AGE), false, false);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> growNaturally(block, random == null ? ThreadLocalRandom.current() : random);

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below != null && below.getState().getType() == BlockTypes.KELP && below.getState().ensureTrait(BlockTraits.KELP_AGE) == MAX_AGE) {
            below.set(below.getState().withTrait(BlockTraits.KELP_AGE, ThreadLocalRandom.current().nextInt(MAX_AGE)), false, false);
        }
    };

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        Level level = block.getLevel();
        Block tip = block;

        while (tip.getPosition().getY() + 1 < level.getMaxHeight()) {
            Block above = level.getLoadedBlock(tip.getPosition().add(0, 1, 0));
            if (above == null) {
                return FertilizationResult.none();
            }

            if (above.getState().getType() != BlockTypes.KELP) {
                return canGrowInto(above) ? new FertilizationResult(List.of(new BlockChange(above, nextTip(tip.getState()))), List.of()) : FertilizationResult.none();
            }

            tip = above;
        }

        return FertilizationResult.none();
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> List.of(ItemStack.from(ItemTypes.KELP));

    public static final PickBlockHandler GET_PICK_BLOCK = block -> ItemStack.from(ItemTypes.KELP);

    private static void growNaturally(Block block, RandomGenerator random) {
        BlockState original = block.getState();
        if (original.ensureTrait(BlockTraits.KELP_AGE) >= MAX_AGE || random.nextDouble() >= GROWTH_CHANCE || block.getPosition().getY() + 1 >= block.getLevel().getMaxHeight()) {
            return;
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        if (above == null || !canGrowInto(above)) {
            return;
        }

        BlockState replaced = above.getState();
        BlockSpreadEvent event = new BlockSpreadEvent(above, block, nextTip(original));
        block.getLevel().getServer().getEventManager().fire(event);
        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == original && block.getLevel().getBlockState(above.getPosition()) == replaced) {
            above.set(event.getNewState());
        }
    }

    private static BlockState nextTip(BlockState state) {
        return state.withTrait(BlockTraits.KELP_AGE, Math.min(state.ensureTrait(BlockTraits.KELP_AGE) + 1, MAX_AGE));
    }

    private static boolean canGrowInto(Block block) {
        return block.getState().getType().isLiquid() && block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
    }
}
