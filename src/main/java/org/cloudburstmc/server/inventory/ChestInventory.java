package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChestInventory extends ContainerInventory {

    protected DoubleChestInventory doubleInventory;

    public ChestInventory(Chest chest) {
        super(chest, InventoryType.CHEST);
    }

    @Override
    public Chest getHolder() {
        return (Chest) this.holder;
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.CHEST_OPEN);
                ContainerInventory.sendBlockEventPacket(this.getHolder(), 1);
            }
        }
    }

    @Override
    public void onClose(CloudPlayer who) {
        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.CHEST_CLOSED);
                ContainerInventory.sendBlockEventPacket(this.getHolder(), 0);
            }
        }
        super.onClose(who);
    }

    public void setDoubleInventory(DoubleChestInventory doubleInventory) {
        this.doubleInventory = doubleInventory;
    }

    public DoubleChestInventory getDoubleInventory() {
        return doubleInventory;
    }

    @Override
    public void sendSlot(int index, CloudPlayer... players) {
        if (this.doubleInventory != null) {
            this.doubleInventory.sendSlot(this, index, players);
        } else {
            super.sendSlot(index, players);
        }
    }
}
