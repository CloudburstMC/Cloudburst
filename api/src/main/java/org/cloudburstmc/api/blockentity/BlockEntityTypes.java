package org.cloudburstmc.api.blockentity;

import lombok.experimental.UtilityClass;

/**
 * Registry keys for built-in block entity types.
 */
@SuppressWarnings("RedundantModifiersUtilityClassLombok")
@UtilityClass
public class BlockEntityTypes {
    public static final BlockEntityType<Banner> BANNER = type("banner", Banner.class);
    public static final BlockEntityType<Barrel> BARREL = type("barrel", Barrel.class);
    public static final BlockEntityType<Beacon> BEACON = type("beacon", Beacon.class);
    public static final BlockEntityType<Bed> BED = type("bed", Bed.class);
    public static final BlockEntityType<Bell> BELL = type("bell", Bell.class);
    public static final BlockEntityType<BlastFurnace> BLAST_FURNACE = type("blast_furnace", BlastFurnace.class);
    public static final BlockEntityType<BrewingStand> BREWING_STAND = type("brewing_stand", BrewingStand.class);
    public static final BlockEntityType<Campfire> CAMPFIRE = type("campfire", Campfire.class);
    public static final BlockEntityType<Cauldron> CAULDRON = type("cauldron", Cauldron.class);
    public static final BlockEntityType<Chest> CHEST = type("chest", Chest.class);
    public static final BlockEntityType<CommandBlock> COMMAND_BLOCK = type("command_block", CommandBlock.class);
    public static final BlockEntityType<Comparator> COMPARATOR = type("comparator", Comparator.class);
    public static final BlockEntityType<Crafter> CRAFTER = type("crafter", Crafter.class);
    public static final BlockEntityType<DaylightDetector> DAYLIGHT_DETECTOR = type("daylight_detector", DaylightDetector.class);
    public static final BlockEntityType<Dispenser> DISPENSER = type("dispenser", Dispenser.class);
    public static final BlockEntityType<Dropper> DROPPER = type("dropper", Dropper.class);
    public static final BlockEntityType<EnchantingTable> ENCHANTING_TABLE = type("enchanting_table", EnchantingTable.class);
    public static final BlockEntityType<EndGateway> END_GATEWAY = type("end_gateway", EndGateway.class);
    public static final BlockEntityType<EndPortal> END_PORTAL = type("end_portal", EndPortal.class);
    public static final BlockEntityType<EnderChest> ENDER_CHEST = type("ender_chest", EnderChest.class);
    public static final BlockEntityType<FlowerPot> FLOWER_POT = type("flower_pot", FlowerPot.class);
    public static final BlockEntityType<Furnace> FURNACE = type("furnace", Furnace.class);
    public static final BlockEntityType<Hopper> HOPPER = type("hopper", Hopper.class);
    public static final BlockEntityType<ItemFrame> ITEM_FRAME = type("item_frame", ItemFrame.class);
    public static final BlockEntityType<Jigsaw> JIGSAW = type("jigsaw", Jigsaw.class);
    public static final BlockEntityType<Jukebox> JUKEBOX = type("jukebox", Jukebox.class);
    public static final BlockEntityType<Lectern> LECTERN = type("lectern", Lectern.class);
    public static final BlockEntityType<MobSpawner> MOB_SPAWNER = type("mob_spawner", MobSpawner.class);
    public static final BlockEntityType<MovingBlock> MOVING_BLOCK = type("moving_block", MovingBlock.class);
    public static final BlockEntityType<NetherReactor> NETHER_REACTOR = type("nether_reactor", NetherReactor.class);
    public static final BlockEntityType<Noteblock> NOTEBLOCK = type("noteblock", Noteblock.class);
    public static final BlockEntityType<Piston> PISTON = type("piston", Piston.class);
    public static final BlockEntityType<ShulkerBox> SHULKER_BOX = type("shulker_box", ShulkerBox.class);
    public static final BlockEntityType<Sign> SIGN = type("sign", Sign.class);
    public static final BlockEntityType<Skull> SKULL = type("skull", Skull.class);
    public static final BlockEntityType<Smoker> SMOKER = type("smoker", Smoker.class);
    public static final BlockEntityType<StructureBlock> STRUCTURE_BLOCK = type("structure_block", StructureBlock.class);

    private static <T extends BlockEntity> BlockEntityType<T> type(String id, Class<T> apiType) {
        return BlockEntityType.from(id, apiType);
    }
}
