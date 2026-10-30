package org.cloudburstmc.server.container;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudContainerItemContainerTest {

    @Test
    void cloudContainerIsDetachedFactoryDefault() {
        ServerNbt.ensureInitialized();

        ItemContainer detached = ItemContainers.detached(27);

        assertInstanceOf(CloudContainer.class, detached);
        assertEquals(27, detached.size());
    }

    @Test
    void cloudContainerSlotOperations() {
        CloudContainer container = new CloudContainer(3);
        container.setItem(0, ItemStack.from(ItemTypes.DIRT, 32));

        ItemStack[] leftover = container.addItem(ItemStack.from(ItemTypes.DIRT, 40));

        assertEquals(0, leftover.length);
        assertEquals(64, container.getItem(0).getCount());
        assertEquals(8, container.getItem(1).getCount());
        assertTrue(container.getItem(2).isEmpty());
        assertEquals(2, container.firstEmpty());

        ItemStack[] overflow = container.addItem(ItemStack.from(ItemTypes.DIRT, 56));

        assertEquals(0, overflow.length);
        assertEquals(64, container.getItem(1).getCount());
        assertTrue(container.getItem(2).isEmpty());
        assertEquals(2, container.firstEmpty());

        ItemStack[] fitted = container.addItem(ItemStack.from(ItemTypes.DIRT, 64));

        assertEquals(0, fitted.length);
        assertEquals(64, container.getItem(2).getCount());

        ItemStack[] homeless = container.addItem(ItemStack.from(ItemTypes.DIRT));

        assertEquals(1, homeless.length);
        assertEquals(1, homeless[0].getCount());

        container.clear();

        assertTrue(container.getItem(0).isEmpty());
        assertTrue(container.getItem(1).isEmpty());
        assertTrue(container.getItem(2).isEmpty());
        assertEquals(0, container.firstEmpty());
    }
}
