package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.ShulkerBoxBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Created by PetteriM1
 */
public class ShulkerBoxInventory extends CloudContainer {

    public ShulkerBoxInventory(ShulkerBoxBlockEntity box) {
        super(box, InventoryType.SHULKER_BOX);
    }

    @Override
    public ShulkerBoxBlockEntity getHolder() {
        return (ShulkerBoxBlockEntity) this.holder;
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.SHULKERBOX_OPEN);
                sendBlockEventPacket(this.getHolder(), 1);
            }
        }
    }

    @Override
    public void onClose(CloudPlayer who) {
        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.SHULKERBOX_CLOSED);
                sendBlockEventPacket(this.getHolder(), 0);
            }
        }

        super.onClose(who);
    }

    @Override
    public boolean canAddItem(ItemStack item) {
        if (item.getType() == BlockTypes.SHULKER_BOX || item.getType() == BlockTypes.UNDYED_SHULKER_BOX) {
            // Do not allow nested shulker boxes.
            return false;
        }
        return super.canAddItem(item);
    }

    @Override
    public void sendSlot(int index, CloudPlayer... players) {
        super.sendSlot(index, players);
    }
}
