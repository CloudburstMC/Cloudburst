package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.impl.ShulkerBoxBlockEntity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.player.Player;

/**
 * Created by PetteriM1
 */
public class ShulkerBoxInventory extends ContainerInventory {

    public ShulkerBoxInventory(ShulkerBoxBlockEntity box) {
        super(box, InventoryType.SHULKER_BOX);
    }

    @Override
    public ShulkerBoxBlockEntity getHolder() {
        return (ShulkerBoxBlockEntity) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            Level level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.SHULKERBOX_OPEN);
                sendBlockEventPacket(this.getHolder(), 1);
            }
        }
    }

    @Override
    public void onClose(Player who) {
        if (this.getViewers().size() == 1) {
            Level level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.SHULKERBOX_CLOSED);
                sendBlockEventPacket(this.getHolder(), 0);
            }
        }

        super.onClose(who);
    }

    @Override
    public boolean canAddItem(ItemStack item) {
        if (item.getId() == BlockIds.SHULKER_BOX || item.getId() == BlockIds.UNDYED_SHULKER_BOX) {
            // Do not allow nested shulker boxes.
            return false;
        }
        return super.canAddItem(item);
    }

    @Override
    public void sendSlot(int index, Player... players) {
        super.sendSlot(index, players);
    }
}
