package org.cloudburstmc.api.container.screen;

import org.cloudburstmc.api.container.view.HotbarView;
import org.cloudburstmc.api.container.view.OffhandView;

public interface HudScreen extends ContainerScreen {

    HotbarView getHotbar();

    OffhandView getOffhand();
}
