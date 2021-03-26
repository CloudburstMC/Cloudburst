package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.blockentity.EnchantingTable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public interface EnchantInventory extends Inventory {

    int ENCHANT_INPUT_SLOT = 14;
    int ENCHANT_REAGENT_SLOT = 15;
    int ENCHANT_OUTPUT_SLOT = 50; //TODO? I think

    @Override
    Player getHolder();

    EnchantingTable getTable();

    ItemStack getInput();

    void setInput(ItemStack item);

    ItemStack getOutput();

    void setOutput(ItemStack item);

    ItemStack getReagent();

    void setReagent(ItemStack item);
}
