package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.PickBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class FrostedIceBlockHandlers {

    private static final int MAX_AGE = 3;
    private static final ThreadLocal<ArrayDeque<Removal>> REMOVALS = ThreadLocal.withInitial(ArrayDeque::new);

    public static final ComplexBlockHandler ON_ADDED = block ->
            block.getLevel().scheduleUpdate(block.getPosition(), ThreadLocalRandom.current().nextInt(60, 121));

    public static final ComplexBlockHandler ON_REMOVE = block ->
            removed(block, ((CloudLevel) block.getLevel()).getDimension() == CloudLevel.DIMENSION_NETHER);

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        int brightness = level.getDimension() == CloudLevel.DIMENSION_THE_END
                ? level.getBlockLightAt(block.getX(), block.getY(), block.getZ()) : level.getFullLight(block.getPosition());
        tick(block, random == null ? ThreadLocalRandom.current() : random, brightness, level.getDimension() == CloudLevel.DIMENSION_NETHER);
    };

    public static final PickBlockHandler GET_PICK_BLOCK = block -> ItemStack.EMPTY;

    public static void removed(Block block, boolean waterEvaporates) {
        ArrayDeque<Removal> removals = REMOVALS.get();
        removals.addLast(new Removal(block, waterEvaporates));
        if (removals.size() > 1) {
            return;
        }

        try {
            while (!removals.isEmpty()) {
                Removal removal = removals.getFirst();
                for (Direction direction : Direction.values()) {
                    Block neighbor = removal.block().getLevel().getLoadedBlock(direction.relative(removal.block().getPosition()));
                    if (neighbor != null && neighbor.getState().getType() == BlockTypes.FROSTED_ICE && hasFewerNeighbors(neighbor, 2)) {
                        IceBlockHandlers.melt(neighbor, removal.waterEvaporates());
                    }
                }
                removals.removeFirst();
            }
        } finally {
            removals.clear();
            REMOVALS.remove();
        }
    }

    public static void tick(Block block, RandomGenerator random, int brightness, boolean waterEvaporates) {
        if ((random.nextInt(3) == 0 || hasFewerNeighbors(block, 4))
                && brightness > 11 - block.getState().ensureTrait(BlockTraits.AGE) - block.getState().getLightDampening()
                && slightlyMelt(block, waterEvaporates)) {
            for (Direction direction : Direction.values()) {
                Block neighbor = block.getLevel().getLoadedBlock(direction.relative(block.getPosition()));
                if (neighbor != null && neighbor.getState().getType() == BlockTypes.FROSTED_ICE && !slightlyMelt(neighbor, waterEvaporates)
                        && neighbor.getLevel().getBlockState(neighbor.getPosition()).getType() == BlockTypes.FROSTED_ICE) {
                    neighbor.getLevel().scheduleUpdate(neighbor.getPosition(), random.nextInt(20, 41));
                }
            }

            return;
        }

        if (block.getLevel().getBlockState(block.getPosition()).getType() == BlockTypes.FROSTED_ICE) {
            block.getLevel().scheduleUpdate(block.getPosition(), random.nextInt(20, 41));
        }
    }

    private static boolean slightlyMelt(Block block, boolean waterEvaporates) {
        int age = block.getState().ensureTrait(BlockTraits.AGE);
        if (age < MAX_AGE) {
            IceBlockHandlers.fade(block, block.getState().withTrait(BlockTraits.AGE, age + 1));
            return false;
        }

        return IceBlockHandlers.melt(block, waterEvaporates) && block.getLevel().getBlockState(block.getPosition()).getType() != BlockTypes.FROSTED_ICE;
    }

    private static boolean hasFewerNeighbors(Block block, int limit) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            Block neighbor = block.getLevel().getLoadedBlock(direction.relative(block.getPosition()));
            if (neighbor == null || neighbor.getState().getType() == BlockTypes.FROSTED_ICE && ++count >= limit) {
                return false;
            }
        }
        return true;
    }

    private record Removal(Block block, boolean waterEvaporates) {
    }
}
