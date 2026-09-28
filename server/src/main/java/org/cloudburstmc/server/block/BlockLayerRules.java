package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.server.block.component.LiquidBlockHandlers;

@UtilityClass
public class BlockLayerRules {

    public static @Nullable BlockLayers resolveReplacement(BlockLayers current, BlockLayer layer, BlockState replacement) {
        return switch (layer) {
            case PRIMARY -> replacePrimary(current, replacement);
            case SECONDARY -> replaceSecondary(current, replacement);
        };
    }

    public static boolean canCoexist(BlockState primary, BlockState secondary) {
        if (secondary == BlockStates.AIR) {
            return true;
        }

        if (secondary.getType().isLiquid()) {
            return canContainLiquid(primary, secondary);
        }

        return primary.getType() == BlockTypes.SNOW_LAYER && secondary.is(BlockTags.SNOWLOGGABLE);
    }

    public static BlockLayers normalizeSnowCover(BlockLayers layers, BlockState support) {
        if (layers.primary().getType() != BlockTypes.SNOW_LAYER) {
            return layers;
        }

        boolean covered = layers.secondary() == BlockStates.AIR && support.getType() == BlockTypes.GRASS_BLOCK;
        BlockState snow = layers.primary().withTrait(BlockTraits.IS_COVERED, covered);
        return snow == layers.primary() ? layers : new BlockLayers(snow, layers.secondary());
    }

    private static BlockLayers replacePrimary(BlockLayers current, BlockState replacement) {
        BlockState primary = current.primary();
        BlockState secondary = current.secondary();

        if (replacement == BlockStates.AIR) {
            if (secondary.is(BlockTags.SNOWLOGGABLE)) {
                return new BlockLayers(secondary, BlockStates.AIR);
            }

            if (secondary.getType().isLiquid() && LiquidState.of(secondary).isSource()) {
                return new BlockLayers(secondary, BlockStates.AIR);
            }

            return new BlockLayers(BlockStates.AIR, BlockStates.AIR);
        }

        if (replacement.getType() == BlockTypes.SNOW_LAYER && primary.is(BlockTags.SNOWLOGGABLE) && secondary == BlockStates.AIR) {
            return new BlockLayers(uncovered(replacement), primary);
        }

        if (secondary == BlockStates.AIR && primary.getType().isLiquid() && LiquidState.of(primary).isSource() && canContainLiquid(replacement, primary)) {
            return new BlockLayers(replacement, primary);
        }

        if (!canCoexist(replacement, secondary)) {
            secondary = BlockStates.AIR;
        }

        return new BlockLayers(replacement, secondary);
    }

    private static @Nullable BlockLayers replaceSecondary(BlockLayers current, BlockState replacement) {
        if (!canCoexist(current.primary(), replacement)) {
            return null;
        }

        return new BlockLayers(current.primary(), replacement);
    }

    private static boolean canContainLiquid(BlockState container, BlockState liquid) {
        if (container == BlockStates.AIR || container.getType().isLiquid()) {
            return false;
        }

        LiquidState state = LiquidState.of(liquid);
        return LiquidBlockHandlers.canOccupySecondaryLayer(state) && (state.isSource() ? container.canContainLiquidSource() : container.canContainFlowingLiquid());
    }

    private static BlockState uncovered(BlockState snow) {
        return snow.withTrait(BlockTraits.IS_COVERED, false);
    }
}
