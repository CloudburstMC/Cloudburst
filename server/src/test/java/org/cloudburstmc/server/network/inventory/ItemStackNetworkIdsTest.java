package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackNetworkIdsTest {

    @Test
    void keepsStackIdentitySeparateFromValueEqualityAndOfferIds() {
        ItemStackNetworkIds ids = new ItemStackNetworkIds();
        ItemStack first = ItemStack.from(ItemTypes.DIAMOND_SWORD);
        ItemStack second = ItemStack.from(ItemTypes.DIAMOND_SWORD);

        int firstId = ids.get(first);
        int secondId = ids.get(second);
        int offerId = ids.allocate();

        assertEquals(first, second);
        assertTrue(firstId > 0 && secondId > 0 && offerId > 0);
        assertEquals(firstId, ids.get(first));
        assertTrue(ids.matches(first, firstId));
        assertFalse(ids.matches(second, firstId));
        assertNotEquals(firstId, secondId);
        assertNotEquals(firstId, offerId);
        assertNotEquals(secondId, offerId);
        assertEquals(0, ids.get(ItemStack.EMPTY));
    }

    @Test
    void sharedStackHasIndependentIdsForItsViewers() {
        ItemStackNetworkIds firstViewer = new ItemStackNetworkIds();
        ItemStackNetworkIds secondViewer = new ItemStackNetworkIds();
        ItemStack shared = ItemStack.from(ItemTypes.DIAMOND_SWORD);
        int firstId = firstViewer.get(shared);

        assertFalse(secondViewer.matches(shared, firstId));
        secondViewer.allocate();
        int secondId = secondViewer.get(shared);

        assertNotEquals(firstId, secondId);
        assertTrue(firstViewer.matches(shared, firstId));
        assertTrue(secondViewer.matches(shared, secondId));
        assertFalse(secondViewer.matches(shared, firstId));
    }
}
