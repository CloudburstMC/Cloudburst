package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.event.level.StructureGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.feature.tree.MangroveTreeGenerator;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class MangrovePropaguleBlockHandlers {

    private static final int MAX_AGE = BlockTraits.PROPAGULE_STAGE.getRange().getEnd();

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            state.withTrait(BlockTraits.IS_HANGING, false).withTrait(BlockTraits.PROPAGULE_STAGE, MAX_AGE);

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        boolean hanging = block.getState().ensureTrait(BlockTraits.IS_HANGING);
        Block support = block.getLevel().getLoadedBlock(block.getPosition().add(0, hanging ? 1 : -1, 0));
        return support != null && support.getState().is(hanging ? BlockTags.SUPPORTS_HANGING_MANGROVE_PROPAGULE : BlockTags.SUPPORTS_MANGROVE_PROPAGULE);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = VegetationBlockHandlers.checkSurvival(CAN_SURVIVE);

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        BlockState state = block.getState();
        if (!CAN_SURVIVE.execute(block)) {
            return;
        }

        RandomGenerator source = random == null ? ThreadLocalRandom.current() : random;
        if (!state.ensureTrait(BlockTraits.IS_HANGING)) {
            if (source.nextInt(7) == 0) {
                growTree(block, source);
            }

            return;
        }

        if (state.ensureTrait(BlockTraits.PROPAGULE_STAGE) >= MAX_AGE) {
            return;
        }

        BlockGrowEvent event = new BlockGrowEvent(block, state.incrementTrait(BlockTraits.PROPAGULE_STAGE));
        block.getLevel().getServer().getEventManager().fire(event);
        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == state) {
            block.set(event.getNewState());
        }
    };

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        BlockState state = block.getState();
        if (!CAN_SURVIVE.execute(block)) {
            return FertilizationResult.none();
        }

        if (!state.ensureTrait(BlockTraits.IS_HANGING)) {
            if (random.nextFloat() >= 0.45f) {
                return FertilizationResult.unchanged();
            }

            MangroveTreeGenerator.Plan plan = MangroveTreeGenerator.generate(block, random);
            return plan == null ? FertilizationResult.unchanged() : new FertilizationResult(plan.blocks(), List.of(), true, plan.type());
        }

        if (state.ensureTrait(BlockTraits.PROPAGULE_STAGE) >= MAX_AGE) {
            return FertilizationResult.none();
        }

        return new FertilizationResult(List.of(new BlockChange(block, state.incrementTrait(BlockTraits.PROPAGULE_STAGE))), List.of());
    };

    public static final FertilizeBlockHandler FERTILIZE_LEAVES = (block, player, random) -> {
        if (block.getPosition().getY() <= block.getLevel().getMinHeight()) {
            return FertilizationResult.none();
        }

        Block below = block.getLevel().getLoadedBlock(Direction.DOWN.relative(block.getPosition()));
        if (below == null || below.getState() != BlockStates.AIR) {
            return FertilizationResult.none();
        }

        BlockState propagule = BlockStates.MANGROVE_PROPAGULE.withTrait(BlockTraits.IS_HANGING, true).withTrait(BlockTraits.PROPAGULE_STAGE, 0);
        return new FertilizationResult(List.of(new BlockChange(below, propagule)), List.of());
    };

    public static final BlockLootHandler GET_LOOT = (block, context) ->
            block.getState().ensureTrait(BlockTraits.PROPAGULE_STAGE) == MAX_AGE ? List.of(ItemStack.from(ItemTypes.MANGROVE_PROPAGULE)) : List.of();

    public static final PickBlockHandler GET_PICK_BLOCK = block -> ItemStack.from(ItemTypes.MANGROVE_PROPAGULE);

    private static void growTree(Block block, RandomGenerator random) {
        MangroveTreeGenerator.Plan plan = MangroveTreeGenerator.generate(block, random);
        if (plan == null) {
            return;
        }

        Level level = block.getLevel();
        StructureGrowEvent event = new StructureGrowEvent(Location.from(block.getPosition(), level), plan.type(), false, null, plan.blocks());
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(block.getPosition()) != block.getState()) {
            return;
        }

        for (BlockChange change : event.getBlocks()) {
            Block current = level.getLoadedBlock(change.block().getPosition());
            if (change.block().getLevel() != level || current == null || current.getState() != change.block().getState()
                    || current.getSecondaryState() != change.block().getSecondaryState()) {
                return;
            }
        }

        level.batchBlockUpdates(() -> {
            for (BlockChange change : event.getBlocks()) {
                change.block().set(change.newState());
            }
        });
    }
}
