package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tracks the original and proposed values of slots read by one inventory request.
 */
public class InventorySlotChanges {

    private final Map<SlotKey, Change> slots = new LinkedHashMap<>();

    public ItemStack read(SlotGroup group, int slot) {
        return this.change(group, slot).proposed;
    }

    public void write(SlotGroup group, int slot, ItemStack item, Consumer<ItemStack> writer) {
        Change change = this.change(group, slot);
        change.proposed = item;
        change.write = () -> writer.accept(item);
    }

    public boolean hasChanges(SlotGroup group, int slot) {
        Change change = this.change(group, slot);
        return !change.original.equals(change.proposed);
    }

    public boolean isStale() {
        for (Change change : this.slots.values()) {
            if (change.group.getItem(change.slot) != change.original) {
                return true;
            }
        }
        return false;
    }

    public void commit(Runnable stationWrites) {
        CloudContainer.applySlotWrites(() -> {
            stationWrites.run();
            for (Change change : this.slots.values()) {
                if (!change.original.equals(change.proposed)) {
                    change.write.run();
                }
            }
        });
    }

    public static SlotKey key(SlotGroup group, int slot) {
        if (group instanceof CloudSlotGroupBase cloudGroup) {
            return new SlotKey(cloudGroup.getContainer(), cloudGroup.getContainerSlot(slot));
        }
        return new SlotKey(group, slot);
    }

    private Change change(SlotGroup group, int slot) {
        return this.slots.computeIfAbsent(key(group, slot), _ -> new Change(group, slot));
    }

    public record SlotKey(Object owner, int slot) {
        @Override
        public boolean equals(Object other) {
            return other instanceof SlotKey(Object otherOwner, int otherSlot) && this.owner == otherOwner && this.slot == otherSlot;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(this.owner) + this.slot;
        }
    }

    private static class Change {
        private final SlotGroup group;
        private final int slot;
        private final ItemStack original;
        private ItemStack proposed;
        private Runnable write = () -> { };

        private Change(SlotGroup group, int slot) {
            this.group = group;
            this.slot = slot;
            this.original = group.getItem(slot);
            this.proposed = this.original;
        }
    }
}
