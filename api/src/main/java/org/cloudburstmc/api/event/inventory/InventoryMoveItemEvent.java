package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;


/**
 * @author CreeperFace
 * <p>
 * Called when inventory transaction is not caused by a player
 */
public final class InventoryMoveItemEvent extends InventoryEvent implements Cancellable {

    private final ContainerView targetInventory;

    private ItemStack item;

    private final Action action;

    public InventoryMoveItemEvent(ContainerView from, ContainerView targetInventory, ItemStack item, Action action) {
        super(from);
        this.targetInventory = targetInventory;
        this.item = item;
        this.action = action;
    }

    public ContainerView getTargetInventory() {
        return targetInventory;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        SLOT_CHANGE, //transaction between 2 inventories
        PICKUP,
        DROP,
        DISPENSE
    }
}
