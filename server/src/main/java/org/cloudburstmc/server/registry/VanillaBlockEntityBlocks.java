package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * Vanilla block families accepted by each block entity type.
 */
@UtilityClass
class VanillaBlockEntityBlocks {
    static final Set<BlockType> BANNERS = Set.of(WALL_BANNER, STANDING_BANNER);
    static final Set<BlockType> BARRELS = Set.of(BARREL);
    static final Set<BlockType> BEACONS = Set.of(BEACON);
    static final Set<BlockType> BEDS = Set.of(BED);
    static final Set<BlockType> BELLS = Set.of(BELL);
    static final Set<BlockType> BLAST_FURNACES = Set.of(BLAST_FURNACE, LIT_BLAST_FURNACE);
    static final Set<BlockType> BREWING_STANDS = Set.of(BREWING_STAND);
    static final Set<BlockType> CAMPFIRES = Set.of(CAMPFIRE, SOUL_CAMPFIRE);
    static final Set<BlockType> CAULDRONS = Set.of(CAULDRON);
    static final Set<BlockType> CHESTS = Set.of(CHEST, TRAPPED_CHEST);
    static final Set<BlockType> COMMAND_BLOCKS = Set.of(COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK);
    static final Set<BlockType> COMPARATORS = Set.of(UNPOWERED_COMPARATOR, POWERED_COMPARATOR);
    static final Set<BlockType> CRAFTERS = Set.of(CRAFTER);
    static final Set<BlockType> DAYLIGHT_DETECTORS = Set.of(DAYLIGHT_DETECTOR, DAYLIGHT_DETECTOR_INVERTED);
    static final Set<BlockType> DISPENSERS = Set.of(DISPENSER);
    static final Set<BlockType> DROPPERS = Set.of(DROPPER);
    static final Set<BlockType> ENCHANTING_TABLES = Set.of(ENCHANTING_TABLE);
    static final Set<BlockType> END_GATEWAYS = Set.of(END_GATEWAY);
    static final Set<BlockType> END_PORTALS = Set.of(END_PORTAL);
    static final Set<BlockType> ENDER_CHESTS = Set.of(ENDER_CHEST);
    static final Set<BlockType> FLOWER_POTS = Set.of(FLOWER_POT);
    static final Set<BlockType> FURNACES = Set.of(FURNACE, LIT_FURNACE);
    static final Set<BlockType> HOPPERS = Set.of(HOPPER);
    static final Set<BlockType> ITEM_FRAMES = Set.of(FRAME, GLOW_FRAME);
    static final Set<BlockType> JIGSAWS = Set.of(JIGSAW);
    static final Set<BlockType> JUKEBOXES = Set.of(JUKEBOX);
    static final Set<BlockType> LECTERNS = Set.of(LECTERN);
    static final Set<BlockType> MOB_SPAWNERS = Set.of(MOB_SPAWNER);
    static final Set<BlockType> MOVING_BLOCKS = Set.of(MOVING_BLOCK);
    static final Set<BlockType> NETHER_REACTORS = Set.of(NETHERREACTOR);
    static final Set<BlockType> NOTE_BLOCKS = Set.of(NOTE_BLOCK);
    static final Set<BlockType> PISTONS = Set.of(PISTON, STICKY_PISTON);
    static final Set<BlockType> SHULKER_BOXES = Set.of(
            UNDYED_SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
            LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX,
            GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX,
            BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX, BLACK_SHULKER_BOX);
    static final Set<BlockType> SIGNS = Set.of(
            ACACIA_HANGING_SIGN, ACACIA_STANDING_SIGN, ACACIA_WALL_SIGN,
            BAMBOO_HANGING_SIGN, BAMBOO_STANDING_SIGN, BAMBOO_WALL_SIGN,
            BIRCH_HANGING_SIGN, BIRCH_STANDING_SIGN, BIRCH_WALL_SIGN,
            CHERRY_HANGING_SIGN, CHERRY_STANDING_SIGN, CHERRY_WALL_SIGN,
            CRIMSON_HANGING_SIGN, CRIMSON_STANDING_SIGN, CRIMSON_WALL_SIGN,
            DARK_OAK_HANGING_SIGN, DARKOAK_STANDING_SIGN, DARKOAK_WALL_SIGN,
            JUNGLE_HANGING_SIGN, JUNGLE_STANDING_SIGN, JUNGLE_WALL_SIGN,
            MANGROVE_HANGING_SIGN, MANGROVE_STANDING_SIGN, MANGROVE_WALL_SIGN,
            OAK_HANGING_SIGN, OAK_STANDING_SIGN, OAK_WALL_SIGN,
            PALE_OAK_HANGING_SIGN, PALE_OAK_STANDING_SIGN, PALE_OAK_WALL_SIGN,
            SPRUCE_HANGING_SIGN, SPRUCE_STANDING_SIGN, SPRUCE_WALL_SIGN,
            WARPED_HANGING_SIGN, WARPED_STANDING_SIGN, WARPED_WALL_SIGN);
    static final Set<BlockType> SKULLS = Set.of(
            SKELETON_SKULL, WITHER_SKELETON_SKULL, ZOMBIE_HEAD, PLAYER_HEAD,
            CREEPER_HEAD, DRAGON_HEAD, PIGLIN_HEAD);
    static final Set<BlockType> SMOKERS = Set.of(SMOKER, LIT_SMOKER);
    static final Set<BlockType> STRUCTURE_BLOCKS = Set.of(STRUCTURE_BLOCK);
    private static final Map<BlockEntityType<?>, Set<BlockType>> BY_TYPE = createIndex();

    static Set<BlockType> get(BlockEntityType<?> type) {
        Set<BlockType> blocks = BY_TYPE.get(type);
        if (blocks == null) {
            throw new IllegalArgumentException("No vanilla block family for " + type.getIdentifier());
        }
        return blocks;
    }

    private static Map<BlockEntityType<?>, Set<BlockType>> createIndex() {
        Map<BlockEntityType<?>, Set<BlockType>> index = new IdentityHashMap<>();
        index.put(BlockEntityTypes.BANNER, BANNERS);
        index.put(BlockEntityTypes.BARREL, BARRELS);
        index.put(BlockEntityTypes.BEACON, BEACONS);
        index.put(BlockEntityTypes.BED, BEDS);
        index.put(BlockEntityTypes.BELL, BELLS);
        index.put(BlockEntityTypes.BLAST_FURNACE, BLAST_FURNACES);
        index.put(BlockEntityTypes.BREWING_STAND, BREWING_STANDS);
        index.put(BlockEntityTypes.CAMPFIRE, CAMPFIRES);
        index.put(BlockEntityTypes.CAULDRON, CAULDRONS);
        index.put(BlockEntityTypes.CHEST, CHESTS);
        index.put(BlockEntityTypes.COMMAND_BLOCK, COMMAND_BLOCKS);
        index.put(BlockEntityTypes.COMPARATOR, COMPARATORS);
        index.put(BlockEntityTypes.CRAFTER, CRAFTERS);
        index.put(BlockEntityTypes.DAYLIGHT_DETECTOR, DAYLIGHT_DETECTORS);
        index.put(BlockEntityTypes.DISPENSER, DISPENSERS);
        index.put(BlockEntityTypes.DROPPER, DROPPERS);
        index.put(BlockEntityTypes.ENCHANTING_TABLE, ENCHANTING_TABLES);
        index.put(BlockEntityTypes.END_GATEWAY, END_GATEWAYS);
        index.put(BlockEntityTypes.END_PORTAL, END_PORTALS);
        index.put(BlockEntityTypes.ENDER_CHEST, ENDER_CHESTS);
        index.put(BlockEntityTypes.FLOWER_POT, FLOWER_POTS);
        index.put(BlockEntityTypes.FURNACE, FURNACES);
        index.put(BlockEntityTypes.HOPPER, HOPPERS);
        index.put(BlockEntityTypes.ITEM_FRAME, ITEM_FRAMES);
        index.put(BlockEntityTypes.JIGSAW, JIGSAWS);
        index.put(BlockEntityTypes.JUKEBOX, JUKEBOXES);
        index.put(BlockEntityTypes.LECTERN, LECTERNS);
        index.put(BlockEntityTypes.MOB_SPAWNER, MOB_SPAWNERS);
        index.put(BlockEntityTypes.MOVING_BLOCK, MOVING_BLOCKS);
        index.put(BlockEntityTypes.NETHER_REACTOR, NETHER_REACTORS);
        index.put(BlockEntityTypes.NOTEBLOCK, NOTE_BLOCKS);
        index.put(BlockEntityTypes.PISTON, PISTONS);
        index.put(BlockEntityTypes.SHULKER_BOX, SHULKER_BOXES);
        index.put(BlockEntityTypes.SIGN, SIGNS);
        index.put(BlockEntityTypes.SKULL, SKULLS);
        index.put(BlockEntityTypes.SMOKER, SMOKERS);
        index.put(BlockEntityTypes.STRUCTURE_BLOCK, STRUCTURE_BLOCKS);
        return Map.copyOf(index);
    }
}
