package org.cloudburstmc.api.inventory;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Registry of all built-in {@link ScreenType} constants.
 *
 * <h2>Type checking: always use {@code ==}, never {@code instanceof}</h2>
 * <p>{@link ScreenType} uses <em>identity equality</em>. Every constant in this class is a unique
 * object. Because several screen types share the same Java interface (see below), you cannot use
 * {@code instanceof} to distinguish e.g. a barrel from a chest; both are {@code StorageScreen}.
 * Always compare the screen's type token with {@code ==}:</p>
 * <pre>{@code
 * // CORRECT: uniquely identifies the barrel screen type
 * if (event.getScreen().getType() == ScreenTypes.BARREL) { ... }
 *
 * // WRONG: also matches CHEST, DOUBLE_CHEST, and SHULKER_BOX
 * if (event.getScreen() instanceof StorageScreen) { ... }
 * }</pre>
 *
 * <h2>Type sharing</h2>
 * <p>Several screen types share the same Java interface class because the containers
 * behave identically from a slot-access perspective:</p>
 * <ul>
 *   <li>{@link #CHEST}, {@link #DOUBLE_CHEST}, {@link #BARREL}, and {@link #SHULKER_BOX} all use
 *       {@link StorageScreen}; use {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlock()}
 *       or {@link org.cloudburstmc.api.inventory.view.SlotGroup#size()} (27 vs 54) to distinguish at runtime.</li>
 *   <li>{@link #FURNACE}, {@link #BLAST_FURNACE}, and {@link #SMOKER} all use {@link FurnaceScreen};
 *       use {@link org.cloudburstmc.api.inventory.view.BlockSlotGroup#getBlock()} to
 *       distinguish them at runtime.</li>
 * </ul>
 * <p>The {@link ScreenType} constants themselves remain distinct objects, so
 * {@code screen.getType() == ScreenTypes.BARREL} always uniquely identifies the screen type
 * even though the Java interface class is shared.</p>
 */
@UtilityClass
public class ScreenTypes {
    public static final ScreenType<AnvilScreen> ANVIL = ScreenType.of(Identifier.parse("cloudburstmc:screen_anvil"), AnvilScreen.class);
    public static final ScreenType<BeaconScreen> BEACON = ScreenType.of(Identifier.parse("cloudburstmc:screen_beacon"), BeaconScreen.class);
    public static final ScreenType<BrewingStandScreen> BREWING_STAND = ScreenType.of(Identifier.parse("cloudburstmc:screen_brewing_stand"), BrewingStandScreen.class);
    public static final ScreenType<CartographyScreen> CARTOGRAPHY = ScreenType.of(Identifier.parse("cloudburstmc:screen_cartography"), CartographyScreen.class);
    public static final ScreenType<CrafterScreen> CRAFTER = ScreenType.of(Identifier.parse("cloudburstmc:screen_crafter"), CrafterScreen.class);
    public static final ScreenType<CraftingTableScreen> CRAFTING_TABLE = ScreenType.of(Identifier.parse("cloudburstmc:screen_crafting_table"), CraftingTableScreen.class);
    public static final ScreenType<DispenserScreen> DISPENSER = ScreenType.of(Identifier.parse("cloudburstmc:screen_dispenser"), DispenserScreen.class);
    public static final ScreenType<DropperScreen> DROPPER = ScreenType.of(Identifier.parse("cloudburstmc:screen_dropper"), DropperScreen.class);
    public static final ScreenType<EnchantingScreen> ENCHANTING = ScreenType.of(Identifier.parse("cloudburstmc:screen_enchanting"), EnchantingScreen.class);
    public static final ScreenType<EnderChestScreen> ENDER_CHEST = ScreenType.of(Identifier.parse("cloudburstmc:screen_ender_chest"), EnderChestScreen.class);
    public static final ScreenType<FurnaceScreen> BLAST_FURNACE = ScreenType.of(Identifier.parse("cloudburstmc:screen_blast_furnace"), FurnaceScreen.class);
    public static final ScreenType<FurnaceScreen> FURNACE = ScreenType.of(Identifier.parse("cloudburstmc:screen_furnace"), FurnaceScreen.class);
    public static final ScreenType<FurnaceScreen> SMOKER = ScreenType.of(Identifier.parse("cloudburstmc:screen_smoker"), FurnaceScreen.class);
    public static final ScreenType<GrindstoneScreen> GRINDSTONE = ScreenType.of(Identifier.parse("cloudburstmc:screen_grindstone"), GrindstoneScreen.class);
    public static final ScreenType<HopperScreen> HOPPER = ScreenType.of(Identifier.parse("cloudburstmc:screen_hopper"), HopperScreen.class);
    public static final ScreenType<HudScreen> HUD = ScreenType.of(Identifier.parse("cloudburstmc:screen_hud"), HudScreen.class);
    public static final ScreenType<LecternScreen> LECTERN = ScreenType.of(Identifier.parse("cloudburstmc:screen_lectern"), LecternScreen.class);
    public static final ScreenType<LoomScreen> LOOM = ScreenType.of(Identifier.parse("cloudburstmc:screen_loom"), LoomScreen.class);
    public static final ScreenType<PlayerInventoryScreen> INVENTORY = ScreenType.of(Identifier.parse("cloudburstmc:screen_inventory"), PlayerInventoryScreen.class);
    public static final ScreenType<SmithingScreen> SMITHING = ScreenType.of(Identifier.parse("cloudburstmc:screen_smithing"), SmithingScreen.class);
    public static final ScreenType<StonecutterScreen> STONECUTTER = ScreenType.of(Identifier.parse("cloudburstmc:screen_stonecutter"), StonecutterScreen.class);
    public static final ScreenType<StorageScreen> BARREL = ScreenType.of(Identifier.parse("cloudburstmc:screen_barrel"), StorageScreen.class);
    public static final ScreenType<StorageScreen> CHEST = ScreenType.of(Identifier.parse("cloudburstmc:screen_chest"), StorageScreen.class);
    public static final ScreenType<StorageScreen> DOUBLE_CHEST = ScreenType.of(Identifier.parse("cloudburstmc:screen_double_chest"), StorageScreen.class);
    public static final ScreenType<StorageScreen> SHULKER_BOX = ScreenType.of(Identifier.parse("cloudburstmc:screen_shulker_box"), StorageScreen.class);
    public static final ScreenType<VirtualChestScreen> VIRTUAL_CHEST = ScreenType.of(Identifier.parse("cloudburstmc:screen_virtual_chest"), VirtualChestScreen.class);
    public static final ScreenType<VirtualDoubleChestScreen> VIRTUAL_DOUBLE_CHEST = ScreenType.of(Identifier.parse("cloudburstmc:screen_virtual_double_chest"), VirtualDoubleChestScreen.class);
    public static final ScreenType<VirtualHopperScreen> VIRTUAL_HOPPER = ScreenType.of(Identifier.parse("cloudburstmc:screen_virtual_hopper"), VirtualHopperScreen.class);
}
