package org.cloudburstmc.api.item;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.block.BlockTagKey;
import org.cloudburstmc.api.block.BlockTags;

/**
 * Standard vanilla tool materials.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolMaterials {
    public static final ToolMaterial WOOD = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15);
    public static final ToolMaterial STONE = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 1.0F, 5);
    public static final ToolMaterial COPPER = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL, 190, 5.0F, 1.0F, 13);
    public static final ToolMaterial IRON = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14);
    public static final ToolMaterial DIAMOND = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10);
    public static final ToolMaterial GOLD = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_GOLD_TOOL, 32, 12.0F, 0.0F, 22);
    public static final ToolMaterial NETHERITE = new VanillaToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15);

    @Getter
    @RequiredArgsConstructor
    private static final class VanillaToolMaterial implements ToolMaterial {
        private final BlockTagKey incorrectBlocksForDrops;
        private final int durability;
        private final float speed;
        private final float attackDamageBonus;
        private final int enchantmentValue;
    }
}
