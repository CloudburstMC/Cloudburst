package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.player.PlayerPickEntityEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemPickControllerTest {

    @Test
    void selectsAnExistingHotbarStackWithoutChangingItsCount() {
        PickInventory inventory = new PickInventory();
        ItemStack eggs = ItemStack.from(ItemTypes.PIG_SPAWN_EGG).withCount(12);
        inventory.setItem(4, eggs);

        pick(inventory, ItemStack.from(ItemTypes.PIG_SPAWN_EGG), false);

        assertEquals(4, inventory.getSelectedSlot());
        assertSame(eggs, inventory.getSelectedItem());
    }

    @Test
    void swapsAnExistingMainInventoryStackIntoTheHotbar() {
        PickInventory inventory = new PickInventory();
        Arrays.fill(inventory.items, ItemStack.from(ItemTypes.DIRT));
        ItemStack previous = inventory.getSelectedItem();
        ItemStack eggs = ItemStack.from(ItemTypes.PIG_SPAWN_EGG).withCount(12);
        inventory.setItem(15, eggs);

        pick(inventory, ItemStack.from(ItemTypes.PIG_SPAWN_EGG), false);

        assertSame(eggs, inventory.getSelectedItem());
        assertSame(previous, inventory.getItem(15));
    }

    @Test
    void doesNotCreateMissingItemsOrSelectItemsWithDifferentComponentsInSurvival() {
        PickInventory inventory = new PickInventory();
        ItemStack damaged = ItemStack.from(ItemTypes.DIAMOND_SWORD).withDamage(10);
        inventory.setItem(3, damaged);

        pick(inventory, ItemStack.from(ItemTypes.DIAMOND_SWORD), false);

        assertEquals(0, inventory.getSelectedSlot());
        assertTrue(inventory.getSelectedItem().isEmpty());
        assertSame(damaged, inventory.getItem(3));
    }

    @Test
    void preservesTheDisplacedStackWhenCreatingAnItemInCreative() {
        PickInventory inventory = new PickInventory();
        ItemStack dirt = ItemStack.from(ItemTypes.DIRT).withCount(32);
        Arrays.fill(inventory.items, 0, inventory.getHotbarSize(), dirt);
        inventory.setSelectedSlot(5);

        pick(inventory, ItemStack.from(ItemTypes.END_CRYSTAL), true);

        assertEquals(5, inventory.getSelectedSlot());
        assertEquals(ItemTypes.END_CRYSTAL, inventory.getSelectedItem().getType());
        assertSame(dirt, inventory.getItem(9));
    }

    @Test
    void searchesForAnEmptyHotbarSlotStartingAtTheSelectedSlot() {
        PickInventory inventory = new PickInventory();
        inventory.setSelectedSlot(7);
        inventory.setItem(7, ItemStack.from(ItemTypes.DIRT));

        pick(inventory, ItemStack.from(ItemTypes.PAINTING), true);

        assertEquals(8, inventory.getSelectedSlot());
        assertEquals(ItemTypes.PAINTING, inventory.getSelectedItem().getType());
        assertEquals(ItemTypes.DIRT, inventory.getItem(7).getType());
    }

    @Test
    void honorsPluginSourceAndTargetSlotOverridesWithoutCreatingItems() {
        PickInventory inventory = new PickInventory();
        ItemStack source = ItemStack.from(ItemTypes.DIRT).withCount(32);
        ItemStack displaced = ItemStack.from(ItemTypes.PAINTING);
        inventory.setItem(15, source);
        inventory.setItem(6, displaced);

        PlayerPickEntityEvent event = new PlayerPickEntityEvent(inventory.getHolder(),
                InterfaceProxy.create(Entity.class),
                ItemStack.from(ItemTypes.PIG_SPAWN_EGG),
                false,
                0,
                -1
        );
        event.setSourceSlot(15);
        event.setTargetSlot(6);

        ItemPickController.pick(inventory, event.getItem(), event.getSourceSlot(), event.getTargetSlot(), false);

        assertEquals(6, inventory.getSelectedSlot());
        assertSame(source, inventory.getSelectedItem());
        assertSame(displaced, inventory.getItem(15));
    }

    @Test
    void replacingThePickedItemRecalculatesItsSourceButPreservesTheTarget() {
        PickInventory inventory = new PickInventory();
        inventory.setItem(20, ItemStack.from(ItemTypes.PAINTING));
        PlayerPickEntityEvent event = new PlayerPickEntityEvent(inventory.getHolder(),
                InterfaceProxy.create(Entity.class),
                ItemStack.from(ItemTypes.PIG_SPAWN_EGG),
                true,
                6,
                -1
        );

        event.setItem(ItemStack.from(ItemTypes.PAINTING));

        assertEquals(20, event.getSourceSlot());
        assertEquals(6, event.getTargetSlot());
        assertTrue(event.isIncludeData());
        assertThrows(IllegalArgumentException.class, () -> event.setSourceSlot(36));
        assertThrows(IllegalArgumentException.class, () -> event.setTargetSlot(9));
    }

    @Test
    void doesNotCreateAnItemWhenAnOverriddenSourceIsEmpty() {
        PickInventory inventory = new PickInventory();

        ItemPickController.pick(inventory, ItemStack.from(ItemTypes.PIG_SPAWN_EGG), 15, 4, true);

        assertEquals(0, inventory.getSelectedSlot());
        assertTrue(inventory.getItem(4).isEmpty());
    }

    private static void pick(PlayerInventoryView inventory, ItemStack item, boolean creative) {
        int source = inventory.first(item);
        ItemPickController.pick(inventory, item, source, ItemPickController.getTargetSlot(inventory, source), creative);
    }

    private static class PickInventory implements PlayerInventoryView {
        private final ItemStack[] items = new ItemStack[36];
        private final Player holder = InterfaceProxy.create(Player.class, Map.of("getInventory", this));
        private int selectedSlot;

        private PickInventory() {
            Arrays.fill(this.items, ItemStack.EMPTY);
        }

        @Override
        public SlotGroupType<? extends PlayerInventoryView> getSlotGroupType() {
            return SlotGroupTypes.INVENTORY;
        }

        @Override
        public int size() {
            return this.items.length;
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.items[slot];
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            this.items[slot] = item;
        }

        @Override
        public Player getHolder() {
            return this.holder;
        }

        @Override
        @NonNull
        public ItemStack getSelectedItem() {
            return this.getItem(this.selectedSlot);
        }

        @Override
        public void setSelectedItem(ItemStack item) {
            this.setItem(this.selectedSlot, item);
        }

        @Override
        public int getSelectedSlot() {
            return this.selectedSlot;
        }

        @Override
        public void setSelectedSlot(int slot) {
            this.selectedSlot = slot;
        }
    }
}
