package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudGrindstoneView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrindstoneSlotMappingTest {

    @Test
    void mapsInputsAndBothResultNamesToBedrockUiSlots() {
        CloudGrindstoneView view = new CloudGrindstoneView(() -> {});

        assertSlot(view, ContainerSlotType.GRINDSTONE_INPUT, CloudGrindstoneView.INPUT_SLOT, 16);
        assertSlot(view, ContainerSlotType.GRINDSTONE_ADDITIONAL, CloudGrindstoneView.ADDITIONAL_SLOT, 17);
        assertSlot(view, ContainerSlotType.GRINDSTONE_RESULT, CloudGrindstoneView.RESULT_SLOT, 50);
        assertSlot(view, ContainerSlotType.CREATED_OUTPUT, CloudGrindstoneView.RESULT_SLOT, 50);
    }

    private static void assertSlot(CloudGrindstoneView view, ContainerSlotType type, int inventorySlot, int protocolSlot) {
        UIContainerMapping mapping = new UIContainerMapping(type, view, inventorySlot);
        assertEquals(inventorySlot, mapping.getInventorySlot(protocolSlot));
        assertEquals(protocolSlot, mapping.getProtocolSlot(inventorySlot));
    }
}
