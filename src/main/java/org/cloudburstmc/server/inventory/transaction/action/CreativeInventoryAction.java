package org.cloudburstmc.server.inventory.transaction.action;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;

/**
 * @author CreeperFace
 */
public class CreativeInventoryAction extends InventoryAction {
    /**
     * Player put an item into the creative window to destroy it.
     */
    public static final int TYPE_DELETE_ITEM = 0;
    /**
     * Player took an item from the creative window.
     */
    public static final int TYPE_CREATE_ITEM = 1;

    protected int actionType;

    public CreativeInventoryAction(ItemStack source, ItemStack target, int action) {
        super(source, target);
    }

    /**
     * Checks that the player is in creative, and (if creating an item) that the item exists in the creative inventory.
     *
     * @param source player
     * @return valid
     */
    public boolean isValid(Player source) {
        return source.isCreative() &&
                (this.actionType == TYPE_DELETE_ITEM || CloudItemRegistry.get().getCreativeItemIndex(this.sourceItem) != -1);
    }

    /**
     * Returns the type of the action.
     *
     * @return action type
     */
    public int getActionType() {
        return actionType;
    }

    /**
     * No need to do anything extra here: this type just provides a place for items to disappear or appear from.
     *
     * @param source playere
     * @return successfully executed
     */
    public boolean execute(Player source) {
        return true;
    }

    public void onExecuteSuccess(Player source) {

    }

    public void onExecuteFail(Player source) {

    }
}
