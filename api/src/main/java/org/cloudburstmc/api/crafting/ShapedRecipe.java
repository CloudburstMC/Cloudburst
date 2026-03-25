package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A crafting recipe where ingredients must be placed in a specific grid pattern.
 * The shape is defined as a 2D arrangement of character keys, each mapped to a
 * {@link RecipeIngredient}, with spaces denoting empty slots.
 */
public interface ShapedRecipe extends CraftingRecipe {

    /**
     * Returns the shape rows in top-to-bottom order. Each character represents one ingredient slot,
     * with a space indicating an empty slot.
     */
    String[] getShape();

    /**
     * Returns the number of columns in this recipe's shape.
     */
    int getWidth();

    /**
     * Returns the number of rows in this recipe's shape.
     */
    int getHeight();

    /**
     * Returns the ingredient at grid position (x, y), or {@link ItemStack#EMPTY} if the slot is empty.
     *
     * <p>This returns only the concrete item stack for matched slots. To check whether a slot accepts
     * a tag-based ingredient (e.g. {@code minecraft:planks}), use {@link #getIngredientChoice(int, int)}
     * instead.
     */
    ItemStack getIngredient(int x, int y);

    /**
     * Returns all non-empty ingredients in row-major order.
     */
    List<? extends ItemStack> getIngredientList();

    /**
     * Returns the full ingredient descriptor at grid position (x, y), preserving tag information.
     * Use this instead of {@link #getIngredient(int, int)} when you need to know whether a slot
     * accepts a tag rather than a single specific item.
     *
     * <p>Returns {@code null} for empty (space) slots in the shape.
     */
    RecipeIngredient getIngredientChoice(int x, int y);

    /**
     * Returns {@code true} if mirror images of this recipe are treated as equivalent during matching.
     * When {@code true}, a horizontally-flipped arrangement will still match the recipe.
     *
     * <p>Defaults to {@code false}.
     */
    default boolean isAssumeSymmetry() {
        return false;
    }

    /**
     * Returns a map from each shape character to its ingredient descriptor.
     * The map contains one entry per unique non-space character in {@link #getShape()},
     * making it easy to inspect what each symbol in the shape pattern requires
     * without iterating by coordinate.
     */
    default Map<Character, RecipeIngredient> getChoiceMap() {
        Map<Character, RecipeIngredient> map = new LinkedHashMap<>();
        String[] shape = getShape();
        for (int y = 0; y < shape.length; y++) {
            String row = shape[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != ' ' && !map.containsKey(c)) {
                    RecipeIngredient choice = getIngredientChoice(x, y);
                    if (choice != null) {
                        map.put(c, choice);
                    }
                }
            }
        }
        return map;
    }
}
