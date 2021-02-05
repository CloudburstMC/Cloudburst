package org.cloudburstmc.server.inventory.transaction.action;

import lombok.ToString;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * @author CreeperFace
 */
@ToString
public abstract class InventoryAction {


    private long creationTime;

    protected ItemStack sourceItem;

    protected ItemStack targetItem;

    public InventoryAction(ItemStack sourceItem, ItemStack targetItem) {
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;

        this.creationTime = System.currentTimeMillis();
    }

    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the item that was present before the action took place.
     *
     * @return source item
     */
    public ItemStack getSourceItem() {
        return sourceItem;
    }

    /**
     * Returns the item that the action attempted to replace the source item with.
     *
     * @return target item
     */
    public ItemStack getTargetItem() {
        return targetItem;
    }

    /**
     * Called by inventory transactions before any actions are processed. If this returns false, the transaction will
     * be cancelled.
     *
     * @param source player
     * @return cancelled
     */
    public boolean onPreExecute(CloudPlayer source) {
        return true;
    }

    /**
     * Returns whether this action is currently valid. This should perform any necessary sanity checks.
     *
     * @param source player
     * @return valid
     */
    abstract public boolean isValid(CloudPlayer source);

    /**
     * Called when the action is added to the specified InventoryTransaction.
     *
     * @param transaction to add
     */
    public void onAddToTransaction(InventoryTransaction transaction) {

    }

    /**
     * Performs actions needed to complete the inventory-action server-side. Returns if it was successful. Will return
     * false if plugins cancelled events. This will only be called if the transaction which it is part of is considered
     * valid.
     *
     * @param source player
     * @return successfully executed
     */
    abstract public boolean execute(CloudPlayer source);

    /**
     * Performs additional actions when this inventory-action completed successfully.
     *
     * @param source player
     */
    abstract public void onExecuteSuccess(CloudPlayer source);

    /**
     * Performs additional actions when this inventory-action did not complete successfully.
     *
     * @param source player
     */
    abstract public void onExecuteFail(CloudPlayer source);
}
