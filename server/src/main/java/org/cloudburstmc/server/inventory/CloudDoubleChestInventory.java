package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryListener;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.Arrays;

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

        ItemStack[] items = new ItemStack[this.left.getSize() + this.right.getSize()];
        // First we add the items from the left chest
        for (int idx = 0; idx < this.left.getSize(); idx++) {
            if (this.left.slots[idx] != ItemStack.EMPTY) { // Don't forget to skip empty slots!
                items[idx] = this.left.slots[idx];
            }
        }
        // And them the items from the right chest
        for (int idx = 0; idx < this.right.getSize(); idx++) {
            if (this.right.slots[idx] != ItemStack.EMPTY) { // Don't forget to skip empty slots!
                items[idx + this.left.getSize()] = this.right.slots[idx]; // idx + this.left.getSize() so we don't overlap left chest items
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
    public ItemStack getItem(int index) {
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
    public ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[this.getSize()];

        for (int i = 0; i < this.getSize(); ++i) {
            contents[i] = this.getItem(i);
        }

        return contents;
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (items.length > this.slots.length) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + this.slots.length + " or less");
        } else if (items.length < this.slots.length) {
            int oldLength = items.length;
            items = Arrays.copyOf(items, this.slots.length);
            Arrays.fill(items, oldLength, items.length, ItemStack.EMPTY);
        }

        this.slots = items;
        this.sendContents(this.getListeners());
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        this.left.listeners.add(who);
        this.right.listeners.add(who);

        if (this.getListeners().size() == 1) {
            CloudLevel level = this.left.getHolder().getLevel();
            if (level != null) {
                CloudContainer.sendBlockEventPacket(this.right.getHolder(), 1);
                level.addLevelSoundEvent(this.left.getHolder().getPosition(), SoundEvent.CHEST_OPEN);
            }
        }
    }

    @Override
    public void onClose(Player who) {
        if (this.getListeners().size() == 1) {
            CloudLevel level = this.right.getHolder().getLevel();
            if (level != null) {
                CloudContainer.sendBlockEventPacket(this.right.getHolder(), 0);
                level.addLevelSoundEvent(this.right.getHolder().getPosition(), SoundEvent.CHEST_CLOSED);
            }
        }

        this.left.listeners.remove(who);
        this.right.listeners.remove(who);
        super.onClose(who);
    }

    public CloudChestInventory getLeftSide() {
        return this.left;
    }

    public CloudChestInventory getRightSide() {
        return this.right;
    }

    public void sendSlot(Inventory inv, int index, InventoryListener... listeners) {
        for (InventoryListener listener : listeners) {
            int idx = inv == this.right ? this.left.getSize() + index : index;
            listener.onInventorySlotChange(this, idx, inv.getItem(idx));
        }
    }
}
