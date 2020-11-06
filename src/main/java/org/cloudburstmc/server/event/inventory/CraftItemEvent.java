package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.inventory.Recipe;
import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CraftItemEvent extends Event implements Cancellable {

    private ItemStack[] input = new ItemStack[0];

    private final Recipe recipe;

    private final Player player;

    private CraftingTransaction transaction;

    public CraftItemEvent(CraftingTransaction transaction) {
        this.transaction = transaction;

        List<ItemStack> merged = new ArrayList<>();
        ItemStack[][] input = transaction.getInputMap();

        for (ItemStack[] items : input) {
            merged.addAll(Arrays.asList(items));
        }
        this.player = transaction.getSource();
        this.input = merged.toArray(new ItemStack[0]);
        this.recipe = transaction.getRecipe();
    }

    public CraftItemEvent(Player player, ItemStack[] input, Recipe recipe) {
        this.player = player;
        this.input = input;
        this.recipe = recipe;
    }

    public CraftingTransaction getTransaction() {
        return transaction;
    }

    public ItemStack[] getInput() {
        return input;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Player getPlayer() {
        return this.player;
    }
}