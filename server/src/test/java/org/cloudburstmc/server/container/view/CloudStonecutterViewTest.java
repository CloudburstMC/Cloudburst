package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloudStonecutterViewTest {

    @Test
    void resultAliasesShareTheBedrockOutputSlot() {
        CloudStonecutterView view = new CloudStonecutterView(InterfaceProxy.create(Block.class));
        UIContainerMapping input = new UIContainerMapping(ContainerSlotType.STONECUTTER_INPUT, view, 0);
        UIContainerMapping result = new UIContainerMapping(ContainerSlotType.STONECUTTER_RESULT, view, 1);
        UIContainerMapping created = new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, view, 1);

        assertEquals(0, input.getInventorySlot(3));
        assertEquals(1, result.getInventorySlot(50));
        assertEquals(1, created.getInventorySlot(50));
        assertThrows(IllegalArgumentException.class, () -> view.setItem(1, ItemStack.EMPTY));
    }

    @Test
    void removingInputClearsTheSelectionAndResult() {
        CloudStonecutterView view = new CloudStonecutterView(InterfaceProxy.create(Block.class));
        StonecuttingRecipe recipe = InterfaceProxy.create(StonecuttingRecipe.class);
        ItemStack input = ItemStack.from(ItemTypes.STONE);
        ItemStack output = ItemStack.from(ItemTypes.COBBLESTONE);
        view.finishCut(input, recipe, output);

        assertSame(recipe, view.getSelectedRecipe());
        assertEquals(output, view.getResult());

        view.setInput(ItemStack.EMPTY);

        assertNull(view.getSelectedRecipe());
        assertTrue(view.getResult().isEmpty());
    }
}
