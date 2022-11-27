package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.inventory.ChestInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudChestInventory extends CloudContainer implements ChestInventory {

    protected CloudDoubleChestInventory doubleInventory;

    public CloudChestInventory(Chest chest) {
        super(chest, InventoryType.CHEST);
    }

    @Override
    public ChestBlockEntity getHolder() {
        return (ChestBlockEntity) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.CHEST_OPEN);
                CloudContainer.sendBlockEventPacket(this.getHolder(), 1);
            }
        }
    }

    @Override
    public void onClose(Player who) {
        if (this.getViewers().size() == 1) {
            CloudLevel level = this.getHolder().getLevel();
            if (level != null) {
                level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.CHEST_CLOSED);
                CloudContainer.sendBlockEventPacket(this.getHolder(), 0);
            }
        }
        super.onClose(who);
    }

    public void setDoubleInventory(CloudDoubleChestInventory doubleInventory) {
        this.doubleInventory = doubleInventory;
    }

    public CloudDoubleChestInventory getDoubleInventory() {
        return doubleInventory;
    }

    @Override
    public void sendSlot(int index, Player... players) {
        if (this.doubleInventory != null) {
            this.doubleInventory.sendSlot(this, index, players);
        } else {
            super.sendSlot(index, players);
        }
    }

    @Override
    public boolean isDouble() {
        return this.doubleInventory != null;
    }
}
