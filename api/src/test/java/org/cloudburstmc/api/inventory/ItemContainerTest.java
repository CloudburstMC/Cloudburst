package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemContainerTest {

    @Test
    void slotGroupIsAnItemContainer() {
        assertTrue(ItemContainer.class.isAssignableFrom(SlotGroup.class),
                "SlotGroup must extend ItemContainer");

        assertEquals(java.util.Set.of("getSlotGroupType"),
                Arrays.stream(SlotGroup.class.getDeclaredMethods())
                        .map(Method::getName)
                        .collect(Collectors.toSet()),
                "SlotGroup must declare only getSlotGroupType");
    }

    @Test
    void detachedContainerWithoutFactoryFailsClearly() {
        ItemContainers.setFactory(null);
        try {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> ItemContainers.detached(3));
            assertTrue(failure.getMessage().contains("factory"),
                    "Failure message must mention the factory, was: " + failure.getMessage());
        } finally {
            ItemContainers.setFactory(null);
        }
    }
}
