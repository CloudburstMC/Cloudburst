package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public interface AnvilInventory extends Inventory {

    @Override
    Player getHolder();

    void setInput(ItemStack item);

    ItemStack getInput();

    void setMaterial(ItemStack item);

    ItemStack getMaterial();

    void setCost(int cost);

    int getCost();


}
