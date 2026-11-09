package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static org.cloudburstmc.server.block.component.SpeleothemBlockHandlers.*;

@UtilityClass
public class IcicleBlockHandlers {

    private static final float GROWTH_CHANCE = 0.011377778f;
    private static final int MAX_GROWTH_LENGTH = 4;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        BlockState placed = SpeleothemBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(state, block, player, face, click);
        if (placed == null) {
            return null;
        }

        Block support = loadedVertical(block, hanging(placed) ? 1 : -1);
        return placed.withTrait(BlockTraits.IS_ATTACHED, support != null && support.getState().getType() != BlockTypes.ICICLE);
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        randomTick(block, random == null ? ThreadLocalRandom.current() : random,
                level.getBlockLightAt(block.getX(), block.getY(), block.getZ()), level.getDimension() == CloudLevel.DIMENSION_NETHER);
    };

    public static void randomTick(Block block, RandomGenerator random, int blockLight, boolean waterEvaporates) {
        if (!isStart(block) && wouldMelt(block, blockLight, waterEvaporates)) {
            Block below = loadedVertical(block, -1);
            if (hanging(block.getState()) && (!isTip(block.getState(), true) || below != null && below.getState() == BlockStates.AIR)) {
                block.requireComponent(BlockComponents.START_FALLING).execute(block);
            } else {
                breakNaturally(block);
            }
        } else if (random.nextFloat() < GROWTH_CHANCE && hanging(block.getState()) && isStart(block)) {
            grow(block, blockLight, waterEvaporates);
        }
    }

    public static void grow(Block start, int blockLight, boolean waterEvaporates) {
        Block support = loadedVertical(start, 1);
        if (start.getState().getType() != BlockTypes.ICICLE || !hanging(start.getState()) || !isStart(start) || support == null
                || support.getState().getType() != BlockTypes.PACKED_ICE || wouldMelt(start, blockLight, waterEvaporates)) {
            return;
        }

        SpeleothemBlockHandlers.growDownward(start, MAX_GROWTH_LENGTH);
    }

    private static boolean wouldMelt(Block block, int blockLight, boolean waterEvaporates) {
        if (waterEvaporates || blockLight > 4) {
            return true;
        }

        for (int distance = 1; distance <= 2; distance++) {
            Block below = loadedVertical(block, -distance);
            if (below != null && below.getState().is(BlockTags.MELTS_ICICLE_ABOVE)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isStart(Block block) {
        Block support = loadedVertical(block, hanging(block.getState()) ? 1 : -1);
        return support != null && support.getState().getType() != BlockTypes.ICICLE;
    }

    private static void breakNaturally(Block block) {
        block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
    }
}
