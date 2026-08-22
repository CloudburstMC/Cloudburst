package org.cloudburstmc.server.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.Tool;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolUtils {

    public static @Nullable Tool getTool(ItemStack item) {
        if (item.isEmpty()) {
            return null;
        }

        return CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.GET_TOOL).execute(item);
    }

    public static float getMiningSpeed(ItemStack item, BlockState block) {
        Tool tool = getTool(item);
        if (tool == null) {
            return 1;
        }

        return tool.getMiningSpeed(block.getType());
    }

    public static boolean isCorrectForDrops(ItemStack item, BlockState block) {
        if (!block.requiresCorrectToolForDrops()) {
            return true;
        }

        Tool tool = getTool(item);
        if (tool == null) {
            return false;
        }

        return tool.isCorrectForDrops(block.getType());
    }

    public static boolean canDestroyInCreative(ItemStack item) {
        Tool tool = getTool(item);
        return tool == null || tool.canDestroyBlocksInCreative();
    }

    public static float getDestroySpeed(Player player, ItemStack item, BlockState block) {
        float speed = getMiningSpeed(item, block);
        if (speed > 1) {
            Enchantment efficiency = item.get(ItemKeys.ENCHANTMENTS).get(EnchantmentTypes.EFFICIENCY);
            if (efficiency != null && efficiency.level() > 0) {
                speed += efficiency.level() * efficiency.level() + 1;
            }
        }

        if (player.hasEffect(EffectTypes.HASTE)) {
            Effect haste = player.getEffect(EffectTypes.HASTE);
            speed *= 1 + (haste.getAmplifier() + 1) * 0.2f;
        }

        if (player.hasEffect(EffectTypes.MINING_FATIGUE)) {
            Effect miningFatigue = player.getEffect(EffectTypes.MINING_FATIGUE);
            speed *= switch (miningFatigue.getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 0.00081f;
            };
        }

        if (player.isInsideOfWater()) {
            speed /= 5;
        }

        if (!player.isOnGround()) {
            speed /= 5;
        }

        return speed;
    }

    public static double getDestroyProgress(Player player, ItemStack item, BlockState block) {
        float hardness = block.getHardness();
        if (hardness < 0) {
            return 0;
        }

        int modifier = isCorrectForDrops(item, block) ? 30 : 100;
        return getDestroySpeed(player, item, block) / hardness / modifier;
    }

    public static int getBreakTicks(Player player, ItemStack item, BlockState block) {
        double progress = getDestroyProgress(player, item, block);
        if (progress <= 0) {
            return 0;
        }

        return (int) Math.ceil(1 / progress);
    }
}
