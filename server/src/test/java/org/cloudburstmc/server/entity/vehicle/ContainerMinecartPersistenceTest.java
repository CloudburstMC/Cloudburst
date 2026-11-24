package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerMinecartPersistenceTest {

    @Test
    void chestMinecartRestoresTwentySevenSlots() {
        EntityContainerMinecart minecart =
                (EntityContainerMinecart) PersistenceFixtures.entity(EntityTypes.CHEST_MINECART);

        assertRestoresInventory(minecart, 27);
    }

    @Test
    void hopperMinecartRestoresFiveSlots() {
        EntityContainerMinecart minecart =
                (EntityContainerMinecart) PersistenceFixtures.entity(EntityTypes.HOPPER_MINECART);

        assertRestoresInventory(minecart, 5);
    }

    @Test
    void hopperMinecartPersistsDisabledStateAndDefaultsToEnabledWithoutTheTag() {
        EntityHopperMinecart minecart = (EntityHopperMinecart) PersistenceFixtures.entity(EntityTypes.HOPPER_MINECART);
        minecart.setEnabled(false);
        NbtMapBuilder output = NbtMap.builder();
        minecart.saveAdditionalData(output);
        NbtMap saved = output.build();
        assertEquals((byte) 0, saved.getByte("Enabled"));

        minecart.setEnabled(true);
        minecart.loadAdditionalData(saved);
        assertFalse(minecart.isEnabled());

        minecart.loadAdditionalData(NbtMap.EMPTY);
        assertTrue(minecart.isEnabled());
    }

    private static void assertRestoresInventory(EntityContainerMinecart minecart, int capacity) {
        StorageView inventory = minecart.getInventory();
        assertEquals(capacity, inventory.size());
        assertFalse(minecart.isRideable());

        int lastSlot = capacity - 1;
        inventory.setItem(lastSlot, ItemStack.builder(ItemTypes.DIAMOND).amount(3).build());
        NbtMapBuilder saved = NbtMap.builder();
        minecart.saveAdditionalData(saved);

        inventory.clear();
        inventory.setItem(0, ItemStack.builder(ItemTypes.GOLD_INGOT).build());
        minecart.loadAdditionalData(saved.build());

        assertSame(inventory, minecart.getInventory());
        assertTrue(inventory.getItem(0).isEmpty());
        assertSame(ItemTypes.DIAMOND, inventory.getItem(lastSlot).getType());
        assertEquals(3, inventory.getItem(lastSlot).getCount());
    }
}
