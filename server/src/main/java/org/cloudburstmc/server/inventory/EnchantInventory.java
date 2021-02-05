package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.CloudPlayer.CraftingType;


/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantInventory extends FakeBlockUIComponent {

    public EnchantInventory(PlayerUIInventory playerUI, Block block) {
        super(playerUI, InventoryType.ENCHANT_TABLE, 14, block);
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);
        who.craftingType = CraftingType.ENCHANT;
    }

    @Override
    public void onClose(CloudPlayer who) {
        super.onClose(who);
        if (this.getViewers().size() == 0) {
            for (int i = 0; i < 2; ++i) {
                who.getInventory().addItem(this.getItem(i));
                this.clear(i);
            }
        }
    }
}
