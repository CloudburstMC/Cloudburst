package org.cloudburstmc.api.container.screen;

import org.cloudburstmc.api.container.view.ArmorView;
import org.cloudburstmc.api.container.view.OffhandView;

public interface InventoryScreen extends UiContainerScreen {

    ArmorView getArmor();

    OffhandView getOffhand();
}
