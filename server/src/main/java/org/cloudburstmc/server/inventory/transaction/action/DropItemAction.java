package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.api.event.player.PlayerDropItemEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * @author CreeperFace
 */
public class DropItemAction extends InventoryAction {

    public DropItemAction(ItemStack source, ItemStack target) {
        super(source, target);
    }

    /**
     * Verifies that the source item of a drop-item action must be air. This is not strictly necessary, just a sanity
     * check.
     */
    public boolean isValid(CloudPlayer source) {
        return this.sourceItem.isNull();
    }

    @Override
    public boolean onPreExecute(CloudPlayer source) {
        PlayerDropItemEvent ev;
        source.getServer().getEventManager().fire(ev = new PlayerDropItemEvent(source, this.targetItem));
        return !ev.isCancelled();
    }

    /**
     * Drops the target item in front of the player.
     */
    public boolean execute(CloudPlayer source) {
        return source.dropItem(this.targetItem);
    }

    public void onExecuteSuccess(CloudPlayer source) {

    }

    public void onExecuteFail(CloudPlayer source) {

    }
}
