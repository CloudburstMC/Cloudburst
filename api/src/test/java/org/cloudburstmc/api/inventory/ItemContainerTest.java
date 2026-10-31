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
    void detachedContainerWithoutFactoryServiceFailsClearly() {
        // The api module ships no ItemContainerFactory service; only the server provides one.
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ItemContainers.detached(3));
        assertTrue(failure.getMessage().contains(ItemContainerFactory.class.getName()),
                "Failure message must name the service, was: " + failure.getMessage());
    }
}
