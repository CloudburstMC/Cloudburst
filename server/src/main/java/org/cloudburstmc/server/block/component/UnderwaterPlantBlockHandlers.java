package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockDestruction;
import org.cloudburstmc.server.block.util.BlockSupport;

import java.util.*;
import java.util.random.RandomGenerator;

@UtilityClass
public class UnderwaterPlantBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (lacksSourceWater(block)) {
            return false;
        }

        SeaGrassType type = block.getState().ensureTrait(BlockTraits.SEA_GRASS_TYPE);
        if (type == SeaGrassType.DOUBLE_TOP) {
            BlockState below = block.getRelativeState(0, -1, 0);
            return below.getType() == BlockTypes.SEAGRASS && below.ensureTrait(BlockTraits.SEA_GRASS_TYPE) == SeaGrassType.DOUBLE_BOTTOM;
        }

        return canGrowOn(block.getLevel(), block.getPosition(), block.getState());
    };

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        if (lacksSourceWater(block) || block.getState().ensureTrait(BlockTraits.SEA_GRASS_TYPE) != SeaGrassType.DEFAULT) {
            return FertilizationResult.none();
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        if (above == null || cannotGrowIn(above)) {
            return FertilizationResult.none();
        }

        return new FertilizationResult(List.of(
                new BlockChange(block, block.getState().withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM)),
                new BlockChange(above, block.getState().withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_TOP))
        ), List.of());
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        SeaGrassType type = block.getState().ensureTrait(BlockTraits.SEA_GRASS_TYPE);
        if (type == SeaGrassType.DOUBLE_TOP && !CAN_SURVIVE.execute(block)) {
            block.set(BlockStates.AIR);
            return;
        }

        boolean missingTop = type == SeaGrassType.DOUBLE_BOTTOM && !isHalf(block.getRelativeState(0, 1, 0), SeaGrassType.DOUBLE_TOP);
        if (missingTop || !CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final BlockDestroyHandler ON_DESTROY = (block, cause) -> {
        SeaGrassType type = block.getState().ensureTrait(BlockTraits.SEA_GRASS_TYPE);
        if (type == SeaGrassType.DEFAULT) {
            BlockDestruction.removePair(block, null);
            return;
        }

        Block partner = block.getRelative(0, type == SeaGrassType.DOUBLE_TOP ? -1 : 1, 0);
        SeaGrassType otherHalf = type == SeaGrassType.DOUBLE_TOP ? SeaGrassType.DOUBLE_BOTTOM : SeaGrassType.DOUBLE_TOP;
        BlockDestruction.removePair(block, isHalf(partner.getState(), otherHalf) ? partner : null);
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (context.tool().getType() != ItemTypes.SHEARS) {
            return List.of();
        }

        int count = block.getState().ensureTrait(BlockTraits.SEA_GRASS_TYPE) == SeaGrassType.DEFAULT ? 1 : 2;
        return List.of(ItemStack.from(ItemTypes.SEAGRASS, count));
    };

    public static FertilizationResult spread(Block origin, Direction face, RandomGenerator random) {
        if (cannotGrowIn(origin)) {
            return FertilizationResult.none();
        }

        Level level = origin.getLevel();
        List<BlockType> plants = growthCandidates(level, BlockTags.UNDERWATER_BONEMEALS);
        List<BlockType> wallCorals = growthCandidates(level, BlockTags.WALL_CORALS);
        Map<Vector3i, BlockChange> changes = new LinkedHashMap<>();

        for (int attempt = 0; attempt < 128; attempt++) {
            Block target = findTarget(origin, attempt / 16, random);
            if (target == null || lacksSourceWater(target)) {
                continue;
            }

            Vector3i position = target.getPosition();
            BlockChange previous = changes.get(position);
            BlockState current = previous == null ? target.getState() : previous.newState();
            if (current.getType() == BlockTypes.SEAGRASS) {
                if (current.ensureTrait(BlockTraits.SEA_GRASS_TYPE) == SeaGrassType.DEFAULT && random.nextInt(10) == 0) {
                    growTall(target, current, changes);
                }

                continue;
            }

            if (!current.getType().isLiquid()) {
                continue;
            }

            BlockState grown = BlockTypes.SEAGRASS.getDefaultState();
            if (attempt == 0 && face.getAxis().isHorizontal() && !wallCorals.isEmpty()) {
                grown = wallCorals.get(random.nextInt(wallCorals.size())).getDefaultState().withTrait(BlockTraits.CORAL_DIRECTION, face);
            } else if (random.nextInt(4) == 0 && !plants.isEmpty()) {
                grown = plants.get(random.nextInt(plants.size())).getDefaultState();
            }

            if (grown.getTraits().containsKey(BlockTraits.CORAL_DIRECTION)) {
                for (int rotation = 0; rotation < 4 && !canGrowOn(level, position, grown); rotation++) {
                    grown = grown.withTrait(BlockTraits.CORAL_DIRECTION, Direction.Plane.HORIZONTAL.random(random));
                }
            }

            if (canGrowOn(level, position, grown)) {
                changes.put(position, new BlockChange(target, grown));
            }
        }

        return new FertilizationResult(new ArrayList<>(changes.values()), List.of());
    }

    private static List<BlockType> growthCandidates(Level level, BlockTagKey tag) {
        return level.getServer().getBlockRegistry().getTag(tag).getValues().stream()
                .sorted(Comparator.comparing(type -> type.getId().toString()))
                .toList();
    }

    private static @Nullable Block findTarget(Block origin, int steps, RandomGenerator random) {
        Block target = origin;
        for (int step = 0; step < steps; step++) {
            Vector3i position = target.getPosition().add(random.nextInt(3) - 1,
                    (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
            target = origin.getLevel().getLoadedBlock(position);
            if (target == null || BlockSupport.isCollisionShapeFullBlock(target.getState())) {
                return null;
            }
        }

        return target;
    }

    private static void growTall(Block target, BlockState state, Map<Vector3i, BlockChange> changes) {
        Block above = target.getLevel().getLoadedBlock(target.getPosition().add(0, 1, 0));
        if (above == null || cannotGrowIn(above) || changes.containsKey(above.getPosition())) {
            return;
        }

        changes.put(target.getPosition(), new BlockChange(target, state.withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_BOTTOM)));
        changes.put(above.getPosition(), new BlockChange(above, state.withTrait(BlockTraits.SEA_GRASS_TYPE, SeaGrassType.DOUBLE_TOP)));
    }

    private static boolean canGrowOn(Level level, Vector3i position, BlockState state) {
        Direction facing = state.getTraits().containsKey(BlockTraits.CORAL_DIRECTION) ? state.ensureTrait(BlockTraits.CORAL_DIRECTION) : Direction.UP;
        Vector3i support = facing.getOpposite().relative(position);
        return (state.getType() != BlockTypes.SEAGRASS || !level.getBlockState(support).is(BlockTags.CANNOT_SUPPORT_SEAGRASS)) && BlockSupport.isFaceSturdy(level, support, facing);
    }

    private static boolean cannotGrowIn(Block block) {
        return !block.getState().getType().isLiquid() || lacksSourceWater(block);
    }

    private static boolean isHalf(BlockState state, SeaGrassType half) {
        return state.getType() == BlockTypes.SEAGRASS && state.ensureTrait(BlockTraits.SEA_GRASS_TYPE) == half;
    }

    private static boolean lacksSourceWater(Block block) {
        LiquidState liquid = block.getLiquid();
        return !liquid.getType().isSameFamily(LiquidTypes.WATER) || !liquid.isSource();
    }
}
