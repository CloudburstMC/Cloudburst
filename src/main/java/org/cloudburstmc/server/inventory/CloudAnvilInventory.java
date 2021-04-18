package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.AnvilInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudAnvilInventory extends BaseInventory implements AnvilInventory {

    public static final int TARGET = 0;
    public static final int SACRIFICE = 1;
    public static final int RESULT = 49;

    private int cost;

    public CloudAnvilInventory(CloudPlayer player) {
        super(player, InventoryType.ANVIL);
    }

    @Override
    public CloudPlayer getHolder() {
        return ((CloudPlayer) super.getHolder());
    }


    @Override
    public void onClose(Player who) {
        super.onClose(who);
        getHolder().getCraftingInventory().resetCraftingGrid();

        for (int i = 0; i < 2; ++i) {
            this.getHolder().getLevel().dropItem(this.getHolder().getPosition().toFloat().add(0.5, 0.5, 0.5), this.getItem(i));
            this.clear(i);
        }
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        //((CloudPlayer) who).craftingType = CraftingType.ANVIL;
    }

    @Override
    public void setInput(ItemStack item) {
        setItem(TARGET, item);
    }

    @Override
    public ItemStack getInput() {
        return getItem(TARGET);
    }

    @Override
    public void setMaterial(ItemStack item) {
        setItem(SACRIFICE, item);
    }

    @Override
    public ItemStack getMaterial() {
        return getItem(SACRIFICE);
    }

    @Override
    public void setCost(int cost) {
        this.cost = cost;
    }

    @Override
    public int getCost() {
        return this.cost;
    }
}
