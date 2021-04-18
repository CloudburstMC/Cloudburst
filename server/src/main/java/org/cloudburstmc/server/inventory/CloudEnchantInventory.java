package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.EnchantingTable;
import org.cloudburstmc.api.inventory.EnchantInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.blockentity.EnchantingTableBlockEntity;
import org.cloudburstmc.server.player.CloudPlayer;


/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudEnchantInventory extends BaseInventory implements EnchantInventory {


    private EnchantingTable table;

    public CloudEnchantInventory(CloudPlayer player, EnchantingTable table) {
        super(player, InventoryType.ENCHANT_TABLE);
        this.table = table;
    }

    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) super.getHolder();
    }

    @Override
    public EnchantingTableBlockEntity getTable() {
        return (EnchantingTableBlockEntity) table;
    }

    @Override
    public ItemStack getInput() {
        return getItem(ENCHANT_INPUT_SLOT);
    }

    @Override
    public void setInput(ItemStack item) {
        setItem(ENCHANT_INPUT_SLOT, item);
    }

    @Override
    public ItemStack getOutput() {
        return getItem(ENCHANT_OUTPUT_SLOT);
    }

    @Override
    public void setOutput(ItemStack item) {
        setItem(ENCHANT_OUTPUT_SLOT, item);
    }

    @Override
    public ItemStack getReagent() {
        return getItem(ENCHANT_REAGENT_SLOT);
    }

    @Override
    public void setReagent(ItemStack item) {
        setItem(ENCHANT_REAGENT_SLOT, item);
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        //((CloudPlayer) who).craftingType = CraftingType.ENCHANT;
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);
        if (this.getViewers().size() == 0) {
            for (int i = 0; i < 2; ++i) {
                who.getInventory().addItem(this.getItem(i));
                this.clear(i);
            }
        }
    }
}
