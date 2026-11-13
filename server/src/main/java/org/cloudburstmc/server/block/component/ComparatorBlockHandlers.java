package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.blockentity.ComparatorBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class ComparatorBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            player == null ? state : state.withTrait(BlockTraits.CARDINAL_DIRECTION, player.getHorizontalDirection().getOpposite().getCardinalDirection());

    public static final ComplexBlockHandler SCHEDULE_UPDATE = block -> block.getLevel().scheduleUpdate(block.getPosition(), 2);

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> SCHEDULE_UPDATE.execute(block);

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (!isComparator(block.refresh())) {
            notifyOutput(block);
        }
    };

    public static final UseBlockHandler USE = (block, player, face, item) -> {
        if (player == null || player.isSpectator() || player.isAdventure()) {
            return false;
        }

        block.set(block.getState().withTrait(BlockTraits.IS_OUTPUT_SUBTRACT, !block.getState().ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT)));
        SCHEDULE_UPDATE.execute(block);

        return true;
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!isComparator(block)) {
            return;
        }

        ComparatorBlockEntity comparator;
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ComparatorBlockEntity existing) {
            comparator = existing;
        } else {
            comparator = (ComparatorBlockEntity) CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.COMPARATOR, block);
        }

        Direction input = inputDirection(block);
        int rear = inputSignal(block, input);
        int side = Math.max(signalFrom(block, input.rotateClockwise()), signalFrom(block, input.rotateCounterClockwise()));
        boolean subtract = block.getState().ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT);
        int output = subtract ? Math.max(0, rear - side) : rear >= side ? rear : 0;
        if (comparator.getOutputSignal() == output && block.getState().ensureTrait(BlockTraits.IS_OUTPUT_LIT) == (output > 0)) {
            return;
        }

        comparator.setOutputSignal(output);
        comparator.setDirty();
        BlockState target = (output > 0 ? BlockTypes.POWERED_COMPARATOR : BlockTypes.UNPOWERED_COMPARATOR)
                .getDefaultState().copyTraits(block.getState()).withTrait(BlockTraits.IS_OUTPUT_LIT, output > 0);
        if (target != block.getState()) {
            block.set(target);
        }
        notifyOutput(block);
    };

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) ->
            face == inputDirection(block).getOpposite()
                    && block.getLevel().getBlockEntity(block.getPosition()) instanceof ComparatorBlockEntity comparator
                    ? comparator.getOutputSignal() : 0;

    public static boolean isComparator(Block block) {
        return block.getState().getType() == BlockTypes.POWERED_COMPARATOR || block.getState().getType() == BlockTypes.UNPOWERED_COMPARATOR;
    }

    private static int inputSignal(Block block, Direction input) {
        Block rear = block.getLevel().getLoadedBlock(input.relative(block.getPosition()));
        if (rear == null) {
            return 0;
        }

        int analog = rear.requireComponent(BlockComponents.GET_COMPARATOR_OUTPUT).execute(rear);
        if (analog >= 0) {
            return analog;
        }

        int signal = signalFrom(block, input);
        if (signal < 15 && rear.requireComponent(BlockComponents.CONDUCTS_REDSTONE).execute(rear.getState())) {
            Block through = block.getLevel().getLoadedBlock(input.relative(rear.getPosition()));
            if (through != null) {
                analog = through.requireComponent(BlockComponents.GET_COMPARATOR_OUTPUT).execute(through);
                if (analog >= 0) {
                    return analog;
                }
            }
        }

        return signal;
    }

    private static int signalFrom(Block block, Direction direction) {
        Block source = block.getLevel().getLoadedBlock(direction.relative(block.getPosition()));
        return source == null ? 0 : source.requireComponent(BlockComponents.GET_REDSTONE_SIGNAL)
                .execute(source, direction.getOpposite());
    }

    private static Direction inputDirection(Block block) {
        return block.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
    }

    private static void notifyOutput(Block block) {
        if (block.getLevel() instanceof CloudLevel level) {
            Direction output = inputDirection(block).getOpposite();
            level.updateAroundRedstone(block.getPosition(), null);
            level.updateAroundRedstone(output.relative(block.getPosition()), output.getOpposite());
        }
    }
}
