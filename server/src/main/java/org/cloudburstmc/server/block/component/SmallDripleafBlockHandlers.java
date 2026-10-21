package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.data.DripleafTilt;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class SmallDripleafBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        if (block.getPosition().getY() + 1 >= block.getLevel().getMaxHeight() || blocksPlanting(block)) {
            return null;
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        if (above == null || blocksPlacement(above)) {
            return null;
        }

        return DefaultPlacementStateHandler.INSTANCE.execute(state, block, player, face, click).withTrait(BlockTraits.IS_UPPER_BLOCK, false);
    };

    public static final BooleanBlockHandler CAN_BE_PLACED = block -> {
        Block existing = block.getLevel().getLoadedBlock(block.getPosition());
        return existing != null && !blocksPlanting(existing);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null) {
            return false;
        }

        if (block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
            return below.getState().getType() == BlockTypes.SMALL_DRIPLEAF_BLOCK && !below.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        }

        return below.getState().is(BlockTags.SUPPORTS_SMALL_DRIPLEAF)
                || below.getState().is(BlockTags.SUPPORTS_VEGETATION)
                && (block.getLiquid().isSource() || block.getLiquid().isFalling())
                && block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
    };

    public static final PlaceBlockHandler ON_PLACE = (state, player, position, face, click) -> {
        if (player == null || position.getY() + 1 >= player.getLevel().getMaxHeight()) {
            return false;
        }

        Level level = player.getLevel();
        Block lower = level.getLoadedBlock(position);
        Block upper = level.getLoadedBlock(position.add(0, 1, 0));
        BlockState bottom = state.withTrait(BlockTraits.IS_UPPER_BLOCK, false);
        BlockState top = state.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
        if (lower == null || upper == null || blocksPlanting(lower) || blocksPlacement(upper)
                || BlockLayerRules.resolveReplacement(new BlockLayers(lower.getState(), lower.getSecondaryState()), BlockLayer.PRIMARY, bottom) == null
                || BlockLayerRules.resolveReplacement(new BlockLayers(upper.getState(), upper.getSecondaryState()), BlockLayer.PRIMARY, top) == null) {
            return false;
        }

        if (!level.setBlockState(position, bottom, false, false)) {
            return false;
        }

        if (!level.setBlockState(upper.getPosition(), top, false, false)) {
            level.setBlockState(position, lower.getState(), false, false);
            level.setBlockState(position, BlockLayer.SECONDARY, lower.getSecondaryState(), false, false);
            return false;
        }

        level.updateAround(position);
        level.updateAround(upper.getPosition());
        return true;
    };

    public static final BlockDestroyHandler ON_DESTROY = VegetationBlockHandlers.DOUBLE_PLANT_DESTROY;

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), 1);
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (hasBrokenStructure(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), 1);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (hasBrokenStructure(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        Level level = block.getLevel();
        Block bottom = block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK) ? level.getLoadedBlock(block.getPosition().add(0, -1, 0)) : block;
        if (bottom == null || bottom.getState().getType() != BlockTypes.SMALL_DRIPLEAF_BLOCK
                || bottom.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK) || !CAN_SURVIVE.execute(bottom)) {
            return FertilizationResult.none();
        }

        Vector3i position = bottom.getPosition();
        Block upper = level.getLoadedBlock(position.add(0, 1, 0));
        if (upper == null || upper.getState().getType() != BlockTypes.SMALL_DRIPLEAF_BLOCK || !upper.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)) {
            return FertilizationResult.none();
        }

        int desiredHeight = random.nextInt(2, 6);
        List<Block> column = new ArrayList<>();
        for (int y = 0; y < desiredHeight && position.getY() + y < level.getMaxHeight(); y++) {
            Block target = level.getLoadedBlock(position.add(0, y, 0));
            if (target == null) {
                return FertilizationResult.none();
            }

            if (BigDripleafBlockHandlers.blocksGrowth(target)) {
                break;
            }

            column.add(target);
        }

        BlockState leaf = BlockTypes.BIG_DRIPLEAF.getDefaultState().withTrait(BlockTraits.DRIPLEAF_HEAD, true)
                .withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.NONE)
                .withTrait(BlockTraits.CARDINAL_DIRECTION, bottom.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        List<BlockChange> changes = new ArrayList<>();
        for (int i = 0; i < column.size(); i++) {
            changes.add(new BlockChange(column.get(i), i == column.size() - 1 ? leaf : BigDripleafBlockHandlers.stem(leaf)));
        }

        return new FertilizationResult(changes, List.of());
    };

    private static boolean blocksPlacement(Block block) {
        return !block.getState().isReplaceable() || block.getState().getType().isLiquid() && !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
    }

    private static boolean blocksPlanting(Block block) {
        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null) {
            return true;
        }

        BlockState soil = below.getState();
        return !soil.is(BlockTags.SUPPORTS_SMALL_DRIPLEAF) && (!soil.is(BlockTags.SUPPORTS_VEGETATION) || !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER));
    }

    private static boolean hasBrokenStructure(Block block) {
        boolean upper = block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        Block partner = block.getLevel().getLoadedBlock(block.getPosition().add(0, upper ? -1 : 1, 0));
        return !CAN_SURVIVE.execute(block) || partner == null || partner.getState().getType() != BlockTypes.SMALL_DRIPLEAF_BLOCK || partner.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK) == upper;
    }
}
