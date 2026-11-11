package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackNetworkIdsTest {

    @Test
    void keepsStackIdentitySeparateFromValueEqualityAndOfferIds() {
        ItemStack first = ItemStack.from(ItemTypes.DIAMOND_SWORD);
        ItemStack second = ItemStack.from(ItemTypes.DIAMOND_SWORD);

        int firstId = ItemStackNetworkIds.get(first);
        int secondId = ItemStackNetworkIds.get(second);
        int offerId = ItemStackNetworkIds.allocate();

        assertEquals(first, second);
        assertTrue(firstId > 0 && secondId > 0 && offerId > 0);
        assertEquals(firstId, ItemStackNetworkIds.get(first));
        assertNotEquals(firstId, secondId);
        assertNotEquals(firstId, offerId);
        assertNotEquals(secondId, offerId);
        assertEquals(0, ItemStackNetworkIds.get(ItemStack.EMPTY));
    }
}
