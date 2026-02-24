package org.cloudburstmc.api.inventory.view;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Registry of all built-in {@link SlotGroupType} constants.
 *
 * <p>Use these as keys when calling
 * {@link org.cloudburstmc.api.inventory.InventoryScreen#getSlots(SlotGroupType)}:</p>
 *
 * <pre>{@code
 * FurnaceView f = screen.getSlots(SlotGroupTypes.FURNACE).orElseThrow();
 * }</pre>
 */
@UtilityClass
public class SlotGroupTypes {
    public static final SlotGroupType<AnvilView> ANVIL = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_anvil"), AnvilView.class);
    public static final SlotGroupType<ArmorView> ARMOR = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_armor"), ArmorView.class);
    public static final SlotGroupType<BlockStorageView> BARREL = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_barrel"), BlockStorageView.class);
    public static final SlotGroupType<BlockBeaconView> BEACON = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_beacon"), BlockBeaconView.class);
    public static final SlotGroupType<BlockFurnaceView> BLAST_FURNACE = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_blast_furnace"), BlockFurnaceView.class);
    public static final SlotGroupType<BlockBrewingStandView> BREWING_STAND = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_brewing_stand"), BlockBrewingStandView.class);
    public static final SlotGroupType<CartographyView> CARTOGRAPHY = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_cartography"), CartographyView.class);
    public static final SlotGroupType<BlockStorageView> CHEST = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_chest"), BlockStorageView.class);
    public static final SlotGroupType<CraftingView> CRAFTING = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_crafting"), CraftingView.class);
    public static final SlotGroupType<CraftingTableView> CRAFTING_TABLE = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_crafting_table"), CraftingTableView.class);
    public static final SlotGroupType<BlockCrafterView> CRAFTER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_crafter"), BlockCrafterView.class);
    public static final SlotGroupType<CursorView> CURSOR = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_cursor"), CursorView.class);
    public static final SlotGroupType<BlockDispenserView> DISPENSER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_dispenser"), BlockDispenserView.class);
    public static final SlotGroupType<BlockStorageView> DOUBLE_CHEST = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_double_chest"), BlockStorageView.class);
    public static final SlotGroupType<BlockDropperView> DROPPER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_dropper"), BlockDropperView.class);
    public static final SlotGroupType<EnchantingView> ENCHANTING = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_enchanting"), EnchantingView.class);
    public static final SlotGroupType<EnderChestView> ENDER_CHEST = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_ender_chest"), EnderChestView.class);
    public static final SlotGroupType<BlockFurnaceView> FURNACE = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_furnace"), BlockFurnaceView.class);
    public static final SlotGroupType<GrindstoneView> GRINDSTONE = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_grindstone"), GrindstoneView.class);
    public static final SlotGroupType<BlockHopperView> HOPPER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_hopper"), BlockHopperView.class);
    public static final SlotGroupType<HotbarView> HOTBAR = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_hotbar"), HotbarView.class);
    public static final SlotGroupType<PlayerInventoryView> INVENTORY = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_inventory"), PlayerInventoryView.class);
    public static final SlotGroupType<BlockLecternView> LECTERN = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_lectern"), BlockLecternView.class);
    public static final SlotGroupType<LoomView> LOOM = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_loom"), LoomView.class);
    public static final SlotGroupType<OffhandView> OFFHAND = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_offhand"), OffhandView.class);
    public static final SlotGroupType<BlockStorageView> SHULKER_BOX = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_shulker_box"), BlockStorageView.class);
    public static final SlotGroupType<SmithingView> SMITHING = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_smithing"), SmithingView.class);
    public static final SlotGroupType<BlockFurnaceView> SMOKER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_smoker"), BlockFurnaceView.class);
    public static final SlotGroupType<StonecutterView> STONECUTTER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_stonecutter"), StonecutterView.class);
    public static final SlotGroupType<StorageView> VIRTUAL_CHEST = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_virtual_chest"), StorageView.class);
    public static final SlotGroupType<StorageView> VIRTUAL_DOUBLE_CHEST = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_virtual_double_chest"), StorageView.class);
    public static final SlotGroupType<HopperView> VIRTUAL_HOPPER = SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_virtual_hopper"), HopperView.class);
}
