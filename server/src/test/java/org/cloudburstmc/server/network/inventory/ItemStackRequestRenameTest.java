package org.cloudburstmc.server.network.inventory;

import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeOptionalAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackRequestRenameTest {

    @Test
    void readsCartographyTextWithoutRequiringAnOptionalCraftAction() {
        ItemStackRequest request = new ItemStackRequest(-1, new ItemStackRequestAction[0], new String[]{"Coast"});
        assertEquals("Coast", InventoryRequest.renameText(request));
    }

    @Test
    void selectsTheExplicitFilterIndexAndRejectsInvalidIndices() {
        ItemStackRequest request = new ItemStackRequest(-1, new ItemStackRequestAction[]{new CraftRecipeOptionalAction(1, 1)}, new String[]{"First", "Second"});
        assertEquals("Second", InventoryRequest.renameText(request));

        ItemStackRequest invalid = new ItemStackRequest(-2, new ItemStackRequestAction[]{new CraftRecipeOptionalAction(1, 2)}, new String[]{"First", "Second"});
        assertThrows(IllegalArgumentException.class, () -> InventoryRequest.renameText(invalid));
    }

    @Test
    void distinguishesClearingANameFromNotRenaming() {
        assertEquals("", InventoryRequest.renameText(new ItemStackRequest(-1, new ItemStackRequestAction[0], new String[]{""})));
        assertNull(InventoryRequest.renameText(new ItemStackRequest(-2, new ItemStackRequestAction[0], new String[0])));
    }
}
