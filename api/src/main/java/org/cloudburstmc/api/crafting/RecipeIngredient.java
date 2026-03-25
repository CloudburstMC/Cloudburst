package org.cloudburstmc.api.crafting;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import java.util.function.Predicate;

/**
 * An ingredient slot in a recipe.
 *
 * <p>Use pattern matching to branch on the concrete type:
 * <pre>{@code
 * switch (ingredient) {
 *     case RecipeIngredient.Exact(var item) -> // one specific item
 *     case RecipeIngredient.Tag(var tag)    -> // any item carrying the given tag
 * }
 * }</pre>
 *
 * <p>{@code test(ItemStack)} works on {@code Exact} variants. On a {@code Tag} variant it
 * always returns {@code false} because tag membership requires registry access that is not
 * available through this interface.
 */
public sealed interface RecipeIngredient extends Predicate<ItemStack> permits RecipeIngredient.Exact, RecipeIngredient.Tag {

    /**
     * An ingredient that requires one specific item type.
     */
    record Exact(Identifier item) implements RecipeIngredient {
        @Override
        public boolean test(ItemStack stack) {
            return !stack.isEmpty() && stack.getType().getId().equals(item);
        }
    }

    /**
     * An ingredient satisfied by any item carrying the given tag.
     * {@code test} always returns {@code false} for this variant.
     */
    record Tag(Identifier tag) implements RecipeIngredient {
        @Override
        public boolean test(ItemStack stack) {
            return false;
        }
    }
}
