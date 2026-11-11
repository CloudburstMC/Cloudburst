package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudSmithingView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmithingSlotMappingTest {

    @Test
    void mapsTheSmithingGridAndBothResultNamesToBedrockUiSlots() {
        CloudSmithingView view = new CloudSmithingView(() -> {});
        assertSlot(view, ContainerSlotType.SMITHING_TABLE_TEMPLATE, CloudSmithingView.TEMPLATE_SLOT, 53);
        assertSlot(view, ContainerSlotType.SMITHING_TABLE_INPUT, CloudSmithingView.BASE_SLOT, 51);
        assertSlot(view, ContainerSlotType.SMITHING_TABLE_MATERIAL, CloudSmithingView.ADDITION_SLOT, 52);
        assertSlot(view, ContainerSlotType.SMITHING_TABLE_RESULT, CloudSmithingView.RESULT_SLOT, 50);
        assertSlot(view, ContainerSlotType.CREATED_OUTPUT, CloudSmithingView.RESULT_SLOT, 50);
    }

    private static void assertSlot(CloudSmithingView view, ContainerSlotType type, int inventorySlot, int protocolSlot) {
        UIContainerMapping mapping = new UIContainerMapping(type, view, inventorySlot);
        assertEquals(inventorySlot, mapping.getInventorySlot(protocolSlot));
        assertEquals(protocolSlot, mapping.getProtocolSlot(inventorySlot));
    }
}
