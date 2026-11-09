package org.cloudburstmc.server.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SlabSlot;

public record BlockPlacementTarget(Block block, Direction placementFace) {

    public static @Nullable BlockPlacementTarget resolve(BlockState placement, Block adjacent, BlockPlacementContext context) {
        Block clicked = context.clickedBlock();

        if (isMatchingSlab(clicked.getState(), placement)) {
            if (canMergeClickedSlab(clicked.getState(), context)) {
                return new BlockPlacementTarget(clicked, context.clickedFace());
            }
        } else if (isMatchingSlab(adjacent.getState(), placement)) {
            return new BlockPlacementTarget(adjacent, context.clickedFace());
        } else if (canReplace(clicked, placement, context)) {
            return new BlockPlacementTarget(clicked, Direction.UP);
        }

        return canReplace(adjacent, placement, context) ? new BlockPlacementTarget(adjacent, context.clickedFace()) : null;
    }

    private static boolean isMatchingSlab(BlockState existing, BlockState placement) {
        return existing.getType() == placement.getType() && existing.is(BlockTags.SLAB) && !existing.is(BlockTags.DOUBLE_SLAB);
    }

    private static boolean canMergeClickedSlab(BlockState existing, BlockPlacementContext context) {
        SlabSlot slot = existing.ensureTrait(BlockTraits.SLAB_SLOT);
        return switch (context.clickedFace()) {
            case UP -> slot == SlabSlot.BOTTOM;
            case DOWN -> slot == SlabSlot.TOP;
            default -> (slot == SlabSlot.BOTTOM) == (context.clickPosition().getY() > 0.5f);
        };
    }

    private static boolean canReplace(Block block, BlockState placement, BlockPlacementContext context) {
        return block.requireComponent(BlockComponents.CAN_BE_REPLACED).execute(block, placement, context);
    }
}
