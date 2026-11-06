package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.LiquidReaction;
import org.cloudburstmc.api.util.VoxelShape;

import java.awt.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Physical properties bound to a canonical block state during registration.
 */
public record BlockStateProperties(
        VoxelShape collisionShape,
        VoxelShape outlineShape,
        float hardness,
        float explosionResistance,
        float friction,
        float translucency,
        float thickness,
        int burnOdds,
        int flameOdds,
        int lightDampening,
        int lightEmission,
        boolean solid,
        boolean requiresCorrectToolForDrops,
        Color mapColor,
        boolean canContainLiquidSource,
        LiquidReaction liquidReaction
) {
    public BlockStateProperties {
        checkNotNull(collisionShape, "collisionShape");
        checkNotNull(outlineShape, "outlineShape");
        checkNotNull(mapColor, "mapColor");
        checkNotNull(liquidReaction, "liquidReaction");
    }
}
