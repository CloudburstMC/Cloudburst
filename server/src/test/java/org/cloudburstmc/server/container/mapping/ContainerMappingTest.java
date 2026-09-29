package org.cloudburstmc.server.container.mapping;

import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContainerMappingTest {

    @Test
    void mapsOnlyTheOffhandProtocolSlot() {
        ContainerMapping mapping = ContainerMapping.offhandView(InterfaceProxy.create(OffhandView.class));

        assertEquals(0, mapping.getInventorySlot(1));
        assertThrows(IllegalArgumentException.class, () -> mapping.getInventorySlot(0));
        assertThrows(IllegalArgumentException.class, () -> mapping.getInventorySlot(2));
    }

    @Test
    void mapsOnlyTheDeclaredRangeWithinASharedView() {
        SlotGroup view = InterfaceProxy.create(SlotGroup.class);
        ContainerMapping mapping = new ContainerMapping(ContainerSlotType.BREWING_RESULT, view, 1, 3, 1);

        assertEquals(1, mapping.getInventorySlot(0));
        assertEquals(3, mapping.getInventorySlot(2));
        assertThrows(IllegalArgumentException.class, () -> mapping.getInventorySlot(-1));
        assertThrows(IllegalArgumentException.class, () -> mapping.getInventorySlot(3));
    }

    @Test
    void mapsAbsoluteProtocolSlotsToTheirSharedViewSlots() {
        SlotGroup view = InterfaceProxy.create(SlotGroup.class);
        ContainerMapping mapping = new ContainerMapping(ContainerSlotType.ANVIL_RESULT, view, 2, 1, -48);

        assertEquals(2, mapping.getInventorySlot(50));
        assertThrows(IllegalArgumentException.class, () -> mapping.getInventorySlot(49));
    }
}
