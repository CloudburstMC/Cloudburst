package org.cloudburstmc.api.container.screen;

import org.cloudburstmc.api.container.view.CursorView;
import org.cloudburstmc.api.container.view.InventoryView;

public interface UiContainerScreen extends ContainerScreen {

    InventoryView getInventory();

    CursorView getCursor();
}
