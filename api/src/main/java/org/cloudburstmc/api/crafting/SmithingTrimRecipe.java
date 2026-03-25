package org.cloudburstmc.api.crafting;

/**
 * A smithing table recipe that applies a trim pattern and material to a piece of armor.
 * The output is determined at runtime based on the armor being trimmed, so
 * {@link Recipe#getResult()} always returns {@link org.cloudburstmc.api.item.ItemStack#EMPTY}.
 */
public interface SmithingTrimRecipe extends SmithingRecipe, ComplexRecipe {
}
