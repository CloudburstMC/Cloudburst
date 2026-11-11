package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InventorySlotChangesTest {

    @Test
    void stagedWritesAreVisibleToTheRequestButNotTheInventoryUntilCommit() {
        AtomicReference<ItemStack> live = new AtomicReference<>(ItemStack.EMPTY);
        SlotGroup group = slotGroup(live);
        ItemStack staged = ItemStack.from(ItemTypes.STICK);
        InventorySlotChanges changes = new InventorySlotChanges();

        changes.write(group, 0, staged, live::set);

        assertSame(staged, changes.read(group, 0));
        assertSame(ItemStack.EMPTY, live.get());
        assertFalse(changes.isStale());

        changes.commit(() -> { });

        assertSame(staged, live.get());
    }

    @Test
    void aLiveChangeInvalidatesThePlan() {
        AtomicReference<ItemStack> live = new AtomicReference<>(ItemStack.EMPTY);
        SlotGroup group = slotGroup(live);
        InventorySlotChanges changes = new InventorySlotChanges();

        changes.read(group, 0);
        live.set(ItemStack.from(ItemTypes.STICK));

        assertTrue(changes.isStale());
    }

    @Test
    void listenersOnlySeeCompletedSlotBatches() {
        CloudContainer source = new CloudContainer(1);
        CloudContainer destination = new CloudContainer(1);
        ItemStack stack = ItemStack.from(ItemTypes.STICK);
        source.setItem(0, stack);

        AtomicInteger notifications = new AtomicInteger();
        source.addContainerListener(new SlotChangeListener(() -> {
            assertEquals(stack, destination.getItem(0));
            notifications.incrementAndGet();
        }));

        assertThrows(IllegalArgumentException.class, () -> CloudContainer.applySlotWrites(() -> {
            source.setItem(0, ItemStack.EMPTY);
            throw new IllegalArgumentException("Rejected write");
        }));
        assertEquals(stack, source.getItem(0));
        assertEquals(0, notifications.get());

        CloudContainer.applySlotWrites(() -> {
            source.setItem(0, ItemStack.EMPTY);
            destination.setItem(0, stack);
        });

        assertEquals(1, notifications.get());
    }

    @Test
    void insertionAndRemovalAccountForRepeatedStacks() {
        CloudContainer inventory = new CloudContainer(2, 10);
        ItemStack stick = ItemStack.from(ItemTypes.STICK);
        inventory.setItem(0, stick.withCount(7));

        ItemStack[] overflow = inventory.addItem(stick.withCount(2), stick.withCount(12));

        assertEquals(10, inventory.getItem(0).getCount());
        assertEquals(10, inventory.getItem(1).getCount());
        assertEquals(1, overflow.length);
        assertEquals(1, overflow[0].getCount());

        ItemStack[] missing = inventory.removeItem(stick.withCount(4), stick.withCount(17));

        assertTrue(inventory.isEmpty());
        assertEquals(1, missing.length);
        assertEquals(1, missing[0].getCount());
    }

    private static SlotGroup slotGroup(AtomicReference<ItemStack> live) {
        return InterfaceProxy.create(SlotGroup.class, (_, method, _) -> {
            if (method.getName().equals("getItem")) {
                return live.get();
            }
            throw new AssertionError("Unexpected slot group call: " + method);
        });
    }

    private record SlotChangeListener(Runnable onChange) implements ContainerListener {
        @Override
        public void onInventoryAdded(Container inventory) {
        }

        @Override
        public void onInventoryRemoved(Container inventory) {
        }

        @Override
        public void onInventorySlotChange(Container inventory, int slot) {
            this.onChange.run();
        }

        @Override
        public void onInventoryContentsChange(Container inventory) {
        }

        @Override
        public void onInventoryDataChange(Container inventory, int property, int value) {
        }
    }
}
