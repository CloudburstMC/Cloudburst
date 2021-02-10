package org.cloudburstmc.api.event.inventory;

import com.google.common.collect.Lists;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class CraftItemEvent extends Event implements Cancellable {

    private final List<ItemStack> input;

    private final Recipe recipe;

    private final Player player;

    //private CraftingTransaction transaction;

    public CraftItemEvent(List<ItemStack> input, Recipe recipe, Player source) {
        this.player = source;
        this.input = input;
        this.recipe = recipe;
    }

    public CraftItemEvent(Player player, ItemStack[] input, Recipe recipe) {
        this.player = player;
        this.input = Lists.newArrayList(input);
        this.recipe = recipe;
    }

    public List<ItemStack> getInput() {
        return input;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Player getPlayer() {
        return this.player;
    }

    public ItemStack getResult() {
        return recipe.getResult();
    }
}