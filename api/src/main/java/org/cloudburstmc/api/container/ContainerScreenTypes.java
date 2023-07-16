package org.cloudburstmc.api.container;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.container.screen.HudScreen;
import org.cloudburstmc.api.container.screen.InventoryScreen;
import org.cloudburstmc.api.container.screen.UiContainerScreen;
import org.cloudburstmc.api.util.Identifier;

@UtilityClass
public class ContainerScreenTypes {

    public static final ContainerScreenType<HudScreen> HUD = ContainerScreenType.from(Identifier.parse("hud"), HudScreen.class);
    public static final ContainerScreenType<UiContainerScreen> ENDER_CHEST = ContainerScreenType.from(Identifier.parse("ender_chest"), UiContainerScreen.class);
    public static final ContainerScreenType<InventoryScreen> INVENTORY = ContainerScreenType.from(Identifier.parse("inventory"), InventoryScreen.class);
}
