package org.cloudburstmc.server.container.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.inventory.view.BlockStonecutterView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.List;

public class CloudStonecutterView extends CloudSlotGroupBase implements BlockStonecutterView {

    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;

    private final Block block;
    private @Nullable StonecuttingRecipe selectedRecipe;

    public CloudStonecutterView(Block block) {
        super(SlotGroupTypes.STONECUTTER, new CloudContainer(RESULT_SLOT + 1));
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    @Override
    public ItemStack getInput() {
        return this.getItem(INPUT_SLOT);
    }

    @Override
    public void setInput(ItemStack item) {
        this.setItem(INPUT_SLOT, item);
    }

    @Override
    public ItemStack getResult() {
        return this.getItem(RESULT_SLOT);
    }

    @Override
    public List<StonecuttingRecipe> getRecipes() {
        return List.copyOf(CloudRecipeRegistry.get().getStonecuttingRecipes(this.getInput()));
    }

    @Override
    public @Nullable StonecuttingRecipe getSelectedRecipe() {
        return this.selectedRecipe;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot == RESULT_SLOT) {
            throw new IllegalArgumentException("Cannot modify a stonecutter result directly");
        }

        ItemStack previous = this.getItem(slot);
        super.setItem(slot, item);
        if (!previous.equals(this.getItem(slot))) {
            this.updateSelection(null, ItemStack.EMPTY);
        }
    }

    public void finishCut(ItemStack remaining, StonecuttingRecipe recipe, ItemStack result) {
        super.setItem(INPUT_SLOT, remaining);
        if (remaining.isEmpty()) {
            this.updateSelection(null, ItemStack.EMPTY);
        } else {
            this.updateSelection(recipe, result);
        }
    }

    public void updateSelection(@Nullable StonecuttingRecipe recipe, ItemStack result) {
        this.selectedRecipe = recipe;
        super.setItem(RESULT_SLOT, result);
    }
}
