package org.cloudburstmc.api.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;

import java.util.random.RandomGenerator;

import static java.util.Objects.requireNonNull;

/**
 * Input used to resolve the item drops and experience produced by a block.
 *
 * @param tool the tool used to break the block
 * @param entity the entity that caused the break, or {@code null} for no entity
 * @param random the random source for loot operations
 */
public record BlockLootContext(ItemStack tool, @Nullable Entity entity, RandomGenerator random) {

    public BlockLootContext {
        requireNonNull(tool, "tool");
        requireNonNull(random, "random");
    }

    /**
     * Creates a context without an entity or tool.
     *
     * @param random the random source for loot operations
     * @return a context for an empty-handed cause without an entity
     */
    public static BlockLootContext empty(RandomGenerator random) {
        return new BlockLootContext(ItemStack.EMPTY, null, random);
    }

    /**
     * Returns the level of an enchantment on the tool.
     *
     * @param type the enchantment type
     * @return the non-negative enchantment level
     */
    public int enchantmentLevel(EnchantmentType type) {
        Enchantment enchantment = this.tool.get(ItemKeys.ENCHANTMENTS).get(requireNonNull(type, "type"));
        return enchantment == null ? 0 : Math.max(0, enchantment.level());
    }
}
