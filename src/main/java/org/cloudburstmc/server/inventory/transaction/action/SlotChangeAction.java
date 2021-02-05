package org.cloudburstmc.server.inventory.transaction.action;

import lombok.ToString;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * @author CreeperFace
 */
@ToString(callSuper = true)
public class SlotChangeAction extends InventoryAction {

    protected Inventory inventory;
    private int inventorySlot;

    public SlotChangeAction(Inventory inventory, int inventorySlot, ItemStack sourceItem, ItemStack targetItem) {
        super(sourceItem, targetItem);
        this.inventory = inventory;
        this.inventorySlot = inventorySlot;
    }

    /**
     * Returns the inventory involved in this action.
     *
     * @return inventory
     */
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Returns the inventorySlot in the inventory which this action modified.
     *
     * @return slot
     */
    public int getSlot() {
        return inventorySlot;
    }

    /**
     * Checks if the item in the inventory at the specified inventorySlot is the same as this action's source item.
     *
     * @param source player
     * @return valid
     */
    public boolean isValid(CloudPlayer source) {
        ItemStack check = inventory.getItem(this.inventorySlot);

        return check.equals(this.sourceItem, true);
    }

    /**
     * Sets the item into the target inventory.
     *
     * @param source player
     * @return successfully executed
     */
    public boolean execute(CloudPlayer source) {
        return this.inventory.setItem(this.inventorySlot, this.targetItem, false);
    }

    /**
     * Sends inventorySlot changes to other viewers of the inventory. This will not send any change back to the source Player.
     *
     * @param source player
     */
    public void onExecuteSuccess(CloudPlayer source) {
        Set<CloudPlayer> viewers = new HashSet<>(this.inventory.getViewers());
        viewers.remove(source);

        this.inventory.sendSlot(this.inventorySlot, viewers);
    }

    /**
     * Sends the original inventorySlot contents to the source player to revert the action.
     *
     * @param source player
     */
    public void onExecuteFail(CloudPlayer source) {
        this.inventory.sendSlot(this.inventorySlot, source);
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        transaction.addInventory(this.inventory);
    }
}
