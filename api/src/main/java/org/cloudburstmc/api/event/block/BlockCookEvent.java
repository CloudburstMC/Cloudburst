package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.crafting.CookingRecipe;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Objects;

/**
 * Called before a cooking block consumes its ingredient and produces a result.
 * Cancelling leaves the ingredient and output unchanged.
 */
public class BlockCookEvent extends BlockEvent implements Cancellable {

    private final ItemStack source;
    private final CookingRecipe recipe;
    private ItemStack result;

    public BlockCookEvent(Block block, ItemStack source, ItemStack result, CookingRecipe recipe) {
        super(block);
        this.source = Objects.requireNonNull(source, "source");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.setResult(result);
    }

    /**
     * @return the ingredient used for this batch
     */
    public ItemStack getSource() {
        return this.source;
    }

    /**
     * @return the recipe being completed
     */
    public CookingRecipe getRecipe() {
        return this.recipe;
    }

    /**
     * @return the output of this batch, not the full output slot
     */
    public ItemStack getResult() {
        return this.result;
    }

    /**
     * Changes the output of this batch.
     *
     * @param result the new output, or {@link ItemStack#EMPTY} for no item
     */
    public void setResult(ItemStack result) {
        this.result = Objects.requireNonNull(result, "result");
    }
}
