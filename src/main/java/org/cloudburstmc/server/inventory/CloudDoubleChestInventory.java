package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudDoubleChestInventory extends CloudContainer implements InventoryHolder {

    private final CloudChestInventory left;
    private final CloudChestInventory right;

    public CloudDoubleChestInventory(ChestBlockEntity left, ChestBlockEntity right) {
        super(null, InventoryType.DOUBLE_CHEST);
        this.holder = this;

        this.left = left.getRealInventory();
        this.left.setDoubleInventory(this);

        this.right = right.getRealInventory();
        this.right.setDoubleInventory(this);

        Map<Integer, ItemStack> items = new HashMap<>();
        // First we add the items from the left chest
        for (int idx = 0; idx < this.left.getSize(); idx++) {
            if (this.left.getContents().containsKey(idx)) { // Don't forget to skip empty slots!
                items.put(idx, this.left.getContents().get(idx));
            }
        }
        // And them the items from the right chest
        for (int idx = 0; idx < this.right.getSize(); idx++) {
            if (this.right.getContents().containsKey(idx)) { // Don't forget to skip empty slots!
                items.put(idx + this.left.getSize(), this.right.getContents().get(idx)); // idx + this.left.getSize() so we don't overlap left chest items
            }
        }

        this.setContents(items);
    }

    @Override
    public Inventory getInventory() {
        return this;
    }

    @Override
    public Chest getHolder() {
        return this.left.getHolder();
    }

    @Override
    public CloudItemStack getItem(int index) {
        return index < this.left.getSize() ? this.left.getItem(index) : this.right.getItem(index - this.right.getSize());
    }

    @Override
    public boolean setItem(int index, ItemStack item, boolean send) {
        return index < this.left.getSize() ? this.left.setItem(index, item, send) : this.right.setItem(index - this.right.getSize(), item, send);
    }

    @Override
    public boolean clear(int index) {
        return index < this.left.getSize() ? this.left.clear(index) : this.right.clear(index - this.right.getSize());
    }

    @Override
    public Map<Integer, ItemStack> getContents() {
        Map<Integer, ItemStack> contents = new HashMap<>();

        for (int i = 0; i < this.getSize(); ++i) {
            contents.put(i, this.getItem(i));
        }

        return contents;
    }

    @Override
    public void setContents(Map<Integer, ItemStack> items) {
        if (items.size() > this.size) {
            Map<Integer, ItemStack> newItems = new HashMap<>();
            for (int i = 0; i < this.size; i++) {
                newItems.put(i, items.get(i));
            }
            items = newItems;
        }

        for (int i = 0; i < this.size; i++) {
            if (!items.containsKey(i)) {
                if (i < this.left.size) {
                    if (this.left.slots.containsKey(i)) {
                        this.clear(i);
                    }
                } else if (this.right.slots.containsKey(i - this.left.size)) {
                    this.clear(i);
                }
            } else if (!this.setItem(i, items.get(i))) {
                this.clear(i);
            }
        }
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        this.left.viewers.add((CloudPlayer) who);
        this.right.viewers.add((CloudPlayer) who);

        if (this.getViewers().size() == 1) {
            CloudLevel level = this.left.getHolder().getLevel();
            if (level != null) {
                CloudContainer.sendBlockEventPacket(this.right.getHolder(), 1);
                level.addLevelSoundEvent(this.left.getHolder().getPosition(), SoundEvent.CHEST_OPEN);
            }
        }
    }

    @Override
    public void onClose(Player who) {
        if (this.getViewers().size() == 1) {
            CloudLevel level = this.right.getHolder().getLevel();
            if (level != null) {
                CloudContainer.sendBlockEventPacket(this.right.getHolder(), 0);
                level.addLevelSoundEvent(this.right.getHolder().getPosition(), SoundEvent.CHEST_CLOSED);
            }
        }

        this.left.viewers.remove(who);
        this.right.viewers.remove(who);
        super.onClose(who);
    }

    public CloudChestInventory getLeftSide() {
        return this.left;
    }

    public CloudChestInventory getRightSide() {
        return this.right;
    }

    public void sendSlot(Inventory inv, int index, Player... players) {

        for (CloudPlayer player : (CloudPlayer[]) players) {
            int id = player.getWindowId(this);
            if (id == -1) {
                this.close(player);
                continue;
            }
            InventorySlotPacket packet = new InventorySlotPacket();
            packet.setSlot(inv == this.right ? this.left.getSize() + index : index);
            packet.setItem(((CloudItemStack) inv.getItem(index)).getNetworkData());
            packet.setContainerId(id);
            player.sendPacket(packet);
        }
    }
}
