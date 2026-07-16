package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.VoxelShape;

import java.awt.*;
import java.util.Set;

/**
 * Server access to block registration operations that are not part of the plugin API.
 */
@UtilityClass
public class BlockRegistrationAccess {

    public static void bindData(BlockState state, Properties properties) {
        state.bindData(properties.collisionShape(), properties.outlineShape(), properties.hardness(),
                properties.explosionResistance(), properties.friction(), properties.translucency(),
                properties.thickness(), properties.burnOdds(), properties.flameOdds(),
                properties.lightDampening(), properties.lightEmission(), properties.solid(),
                properties.requiresCorrectToolForDrops(), properties.mapColor(),
                properties.canContainLiquidSource(), properties.liquidReaction());
    }

    public static void bindTags(BlockType type, Set<BlockTagKey> tags) {
        type.bindTags(tags);
    }

    public static void linkItem(BlockType type, ItemType itemType) {
        type.linkItemType(itemType);
    }

    public static void bindLiquidType(BlockType blockType, LiquidType liquidType) {
        blockType.bindLiquidType(liquidType);
    }

    public record Properties(
            VoxelShape collisionShape,
            @Nullable VoxelShape outlineShape,
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
    }
}
