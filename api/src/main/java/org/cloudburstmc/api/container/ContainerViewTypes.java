package org.cloudburstmc.api.container;

import org.cloudburstmc.api.blockentity.*;
import org.cloudburstmc.api.container.view.*;
import org.cloudburstmc.api.util.Identifier;

public class ContainerViewTypes {

    public static final ContainerViewType<AnvilView> ANVIL = ContainerViewType.from(Identifier.parse("anvil"), AnvilView.class);
    public static final ContainerViewType<ArmorView> ARMOR = ContainerViewType.from(Identifier.parse("armor"), ArmorView.class);
    public static final ContainerViewType<Barrel> BARREL = ContainerViewType.from(Identifier.parse("barrel"), Barrel.class);
    public static final ContainerViewType<BeaconView> BEACON = ContainerViewType.from(Identifier.parse("beacon"), BeaconView.class);
    public static final ContainerViewType<BrewingStand> BREWING_STAND = ContainerViewType.from(Identifier.parse("brewing_stand"), BrewingStand.class);
    public static final ContainerViewType<Chest> CHEST = ContainerViewType.from(Identifier.parse("chest"), Chest.class);
    public static final ContainerViewType<CraftingView> CRAFTING = ContainerViewType.from(Identifier.parse("crafting"), CraftingView.class);
    public static final ContainerViewType<CursorView> CURSOR = ContainerViewType.from(Identifier.parse("cursor"), CursorView.class);
    public static final ContainerViewType<Dispenser> DISPENSER = ContainerViewType.from(Identifier.parse("dispenser"), Dispenser.class);
    public static final ContainerViewType<Dropper> DROPPER = ContainerViewType.from(Identifier.parse("dropper"), Dropper.class);
    public static final ContainerViewType<EnchantView> ENCHANT = ContainerViewType.from(Identifier.parse("enchant"), EnchantView.class);
    public static final ContainerViewType<EnderChestView> ENDER_CHEST = ContainerViewType.from(Identifier.parse("ender_chest"), EnderChestView.class);
    public static final ContainerViewType<Furnace> FURNACE = ContainerViewType.from(Identifier.parse("furnace"), Furnace.class);
    public static final ContainerViewType<Hopper> HOPPER = ContainerViewType.from(Identifier.parse("hopper"), Hopper.class);
    public static final ContainerViewType<ShulkerBox> SHULKER_BOX = ContainerViewType.from(Identifier.parse("shulker_box"), ShulkerBox.class);
    public static final ContainerViewType<Smoker> SMOKER = ContainerViewType.from(Identifier.parse("smoker"), Smoker.class);
    public static final ContainerViewType<HotbarView> HOTBAR = ContainerViewType.from(Identifier.parse("hotbar"), HotbarView.class);
    public static final ContainerViewType<InventoryView> INVENTORY = ContainerViewType.from(Identifier.parse("inventory"), InventoryView.class);
    public static final ContainerViewType<OffhandView> OFFHAND = ContainerViewType.from(Identifier.parse("offhand"), OffhandView.class);
}
