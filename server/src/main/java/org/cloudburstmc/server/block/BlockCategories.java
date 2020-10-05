package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Set;

import static org.cloudburstmc.server.block.BlockTypes.*;

@UtilityClass
public class BlockCategories {

    private final EnumMap<BlockCategory, ReferenceSet<BlockType>> categoryMap = new EnumMap<>(BlockCategory.class);

    static {
        init();
        initDefaultCategories();
    }

    public void init() {
        for (BlockCategory category : BlockCategory.values()) {
            categoryMap.put(category, new ReferenceOpenHashSet<>());
        }
    }

    private void initDefaultCategories() {
        categorize(BlockCategory.SOLID,
                ALLOW,
                ANCIENT_DEBRIS,
                ANVIL,
                BARREL,
                BARRIER,
                BASALT,
                BEACON,
                BED,
                BEDROCK,
                BEEHIVE,
                BEE_NEST,
                BELL,
                BLACKSTONE,
                BLAST_FURNACE,
                BLUE_ICE,
                BONE_BLOCK,
                BOOKSHELF,
                BORDER_BLOCK,
                BREWING_STAND,
                BRICK_BLOCK,
                BRICK_STAIRS,
                BROWN_MUSHROOM_BLOCK,
                CACTUS,
                CAKE,
                CAMERA,
                CARTOGRAPHY_TABLE,
                CARVED_PUMPKIN,
                CAULDRON,
                CHAIN,
                CHAIN_COMMAND_BLOCK,
                CHEMICAL_HEAT,
                CHEMISTRY_TABLE,
                CHEST,
                CHORUS_FLOWER,
                CHORUS_PLANT,
                CLAY,
                COAL_BLOCK,
                COAL_ORE,
                COBBLESTONE,
                COBBLESTONE_WALL,
                COMMAND_BLOCK,
                COMPOSTER,
                CONCRETE,
                CONCRETE_POWDER,
                CORAL,
                CORAL_BLOCK,
                CORAL_FAN,
                CORAL_FAN_DEAD,
                CORAL_FAN_HANG,
                CRAFTING_TABLE,
                CRYING_OBSIDIAN,
                DARK_PRISMARINE_STAIRS,
                DAYLIGHT_DETECTOR,
                DENY,
                DIAMOND_BLOCK,
                DIAMOND_ORE,
                DIRT,
                DISPENSER,
                DOUBLE_STONE_SLAB,
                DRAGON_EGG,
                DRIED_KELP_BLOCK,
                DROPPER,
                ELEMENT_0,
                EMERALD_BLOCK,
                EMERALD_ORE,
                ENCHANTING_TABLE,
                ENDER_CHEST,
                END_BRICKS,
                END_PORTAL_FRAME,
                END_STONE,
                FARMLAND,
                WOODEN_FENCE,
                WOODEN_FENCE_GATE,
                FLETCHING_TABLE,
                FROSTED_ICE,
                FURNACE,
                GILDED_BLACKSTONE,
                GLASS,
                GLASS_PANE,
                GLOWING_OBSIDIAN,
                GLOWSTONE,
                GOLD_BLOCK,
                GOLD_ORE,
                GRASS,
                GRASS_PATH,
                GRAVEL,
                GRINDSTONE,
                HARDENED_CLAY,
                HARD_GLASS,
                HARD_GLASS_PANE,
                HARD_STAINED_GLASS,
                HARD_STAINED_GLASS_PANE,
                HAY_BLOCK,
                HEAVY_WEIGHTED_PRESSURE_PLATE,
                HONEYCOMB_BLOCK,
                HONEY_BLOCK,
                HOPPER,
                ICE,
                INFO_UPDATE,
                INFO_UPDATE2,
                INVISIBLE_BEDROCK,
                IRON_BARS,
                IRON_BLOCK,
                IRON_DOOR,
                IRON_ORE,
                IRON_TRAPDOOR,
                JIGSAW,
                JUKEBOX,
                LANTERN,
                LAPIS_BLOCK,
                LAPIS_ORE,
                LEAVES,
                LECTERN,
                LIGHT_WEIGHTED_PRESSURE_PLATE,
                LODESTONE,
                LOG,
                LOOM,
                MAGMA,
                MELON_BLOCK,
                MOB_SPAWNER,
                MONSTER_EGG,
                MOSSY_COBBLESTONE,
                MOVING_BLOCK,
                MYCELIUM,
                NETHERITE_BLOCK,
                NETHERRACK,
                NETHER_REACTOR,
                NETHER_BRICK,
                NETHER_BRICK_FENCE,
                NETHER_BRICK_STAIRS,
                NETHER_GOLD_ORE,
                NETHER_WART_BLOCK,
                NOTEBLOCK,
                WOODEN_STAIRS,
                OBSERVER,
                OBSIDIAN,
                PACKED_ICE,
                PISTON,
                PISTON_ARM_COLLISION,
                PLANKS,
                PODZOL,
                POLISHED_BLACKSTONE,
                POLISHED_BLACKSTONE_BRICKS,
                PRISMARINE,
                PRISMARINE_BRICKS_STAIRS,
                PRISMARINE_STAIRS,
                PUMPKIN,
                PURPUR_BLOCK,
                PURPUR_STAIRS,
                QUARTZ_BLOCK,
                QUARTZ_ORE,
                QUARTZ_STAIRS,
                REDSTONE_BLOCK,
                REDSTONE_LAMP,
                REDSTONE_ORE,
                RED_MUSHROOM_BLOCK,
                RED_NETHER_BRICK,
                RED_SANDSTONE,
                RED_SANDSTONE_STAIRS,
                REPEATING_COMMAND_BLOCK,
                RESERVED6,
                RESPAWN_ANCHOR,
                SAND,
                SANDSTONE,
                SANDSTONE_STAIRS,
                SEA_LANTERN,
                SHROOMLIGHT,
                SHULKER_BOX,
                SLIME,
                SMITHING_TABLE,
                SMOKER,
                SNOW,
                SOUL_SAND,
                SOUL_SOIL,
                SPONGE,
                STAINED_GLASS,
                STAINED_GLASS_PANE,
                STAINED_HARDENED_CLAY,
                STANDING_BANNER,
                STANDING_SIGN,
                STICKY_PISTON,
                STONE,
                STONEBRICK,
                STONECUTTER,
                STONECUTTER_BLOCK,
                STONE_BRICK_STAIRS,
                STONE_BUTTON,
                STONE_PRESSURE_PLATE,
                STONE_SLAB,
                STONE_STAIRS,
                STRUCTURE_BLOCK,
//                STRUCTURE_VOID,
                TARGET,
                TNT,
                WOODEN_TRAPDOOR,
                TRAPPED_CHEST,
                TURTLE_EGG,
                UNDYED_SHULKER_BOX,
                WALL_BANNER,
                WALL_SIGN,
                GLAZED_TERRACOTTA,
                WOOD,
                WOODEN_BUTTON,
                WOODEN_DOOR,
                WOODEN_PRESSURE_PLATE,
                WOODEN_SLAB,
                WOOL
        );

        categorize(BlockCategory.TRANSPARENT,
                ANVIL,
                BAMBOO,
                BEACON,
                BARRIER,
                FROSTED_ICE,
                GLOWSTONE,
                SEA_LANTERN,
                SHULKER_BOX,
                ICE,
                LIT_PUMPKIN,
                LEAVES,
                REDSTONE_LAMP,
                TNT,
                OBSERVER,
                BED,
                BELL,
                BREWING_STAND,
                BUBBLE_COLUMN,
                CACTUS,
                CAKE,
                CAMPFIRE,
                CARPET,
                CAULDRON,
                CHAIN,
                CHEST,
                CHORUS_FLOWER,
                WEB,
                COCOA,
                CONDUIT,
                DRAGON_EGG,
                ENCHANTING_TABLE,
                END_ROD,
                ENDER_CHEST,
                TRAPPED_CHEST,
                FARMLAND,
                FLOWER_POT,
                GLASS_PANE,
                GRINDSTONE,
                SKULL,
                HONEY_BLOCK,
                IRON_BARS,
                LADDER,
                LANTERN,
                LECTERN,
                WATERLILY,
                SCAFFOLDING,
                SEA_PICKLE,
                SNOW,
                SNOW_LAYER,
                STONECUTTER,
                SWEET_BERRY_BUSH,
                TURTLE_EGG,
                DAYLIGHT_DETECTOR,
                HOPPER,
                PISTON_ARM_COLLISION,
                MOVING_BLOCK,
                REPEATER,
                COMPARATOR,
                AIR,
                WALL_BANNER,
                STANDING_BANNER,
                CORAL,
                CORAL_FAN_HANG,
                CORAL_FAN,
                CORAL_FAN_DEAD,
                DEADBUSH,
                END_GATEWAY,
                END_PORTAL,
                FIRE,
                DOUBLE_PLANT,
                FIRE,
                CRIMSON_FUNGUS,
                RED_FLOWER,
                YELLOW_FLOWER,
                TALL_GRASS,
                LIGHT_BLOCK,
                BROWN_MUSHROOM,
                RED_MUSHROOM,
                PORTAL,
                NETHER_SPROUTS,
                NETHER_WART,
                CRIMSON_ROOTS,
                SAPLING,
//                BAMBOO_SAPLING,
                SEAGRASS,
//                STRUCTURE_VOID,
                REEDS,
                TORCH,
                TWISTING_VINES,
                VINE,
                WEEPING_VINES,
                LEVER,
                REDSTONE_WIRE,
                REDSTONE_TORCH,
                TRIPWIRE,
                TRIPWIRE_HOOK,
                END_PORTAL_FRAME,
                INVISIBLE_BEDROCK
        );

        categorize(BlockCategory.STAIRS,
                WOODEN_STAIRS,
                BRICK_STAIRS,
                DARK_PRISMARINE_STAIRS,
                NETHER_BRICK_STAIRS,
                PRISMARINE_BRICKS_STAIRS,
                PRISMARINE_STAIRS,
                PURPUR_STAIRS,
                QUARTZ_STAIRS,
                RED_SANDSTONE_STAIRS,
                SANDSTONE_STAIRS,
                STONE_BRICK_STAIRS,
                STONE_STAIRS
        );
        categorize(BlockCategory.SLAB,
                STONE_SLAB,
                WOODEN_SLAB
        );

        categorize(BlockCategory.DOUBLE_SLAB,
                DOUBLE_STONE_SLAB,
                DOUBLE_WOODEN_SLAB
        );

        categorize(BlockCategory.DOOR,
                IRON_DOOR,
                WOODEN_DOOR
        );

        categorize(BlockCategory.TRAPDOOR,
                IRON_TRAPDOOR,
                WOODEN_TRAPDOOR
        );

        categorize(BlockCategory.LIQUID,
                WATER,
                FLOWING_WATER,
                LAVA,
                FLOWING_LAVA
        );

        categorize(BlockCategory.CROPS,
                WHEAT,
                BEETROOT,
                CARROTS,
                POTATOES,
                MELON_STEM,
                PUMPKIN_STEM,
                BAMBOO,
                COCOA,
                REEDS,
                SWEET_BERRY_BUSH,
                CACTUS,
                BROWN_MUSHROOM,
                RED_MUSHROOM,
                KELP,
                SEA_PICKLE,
                NETHER_WART,
                CHORUS_PLANT
        );

        categorize(BlockCategory.WALLS,
                COBBLESTONE_WALL
        );

        categorize(BlockCategory.SIGN,
                WALL_SIGN,
                STANDING_SIGN
        );

        categorize(BlockCategory.STEM,
                PUMPKIN_STEM,
                MELON_STEM
        );

        categorize(BlockCategory.BUTTON,
                STONE_BUTTON,
                WOODEN_BUTTON
        );

        categorize(BlockCategory.PRESSURE_PLATE,
                LIGHT_WEIGHTED_PRESSURE_PLATE,
                HEAVY_WEIGHTED_PRESSURE_PLATE,
                STONE_PRESSURE_PLATE,
                WOODEN_PRESSURE_PLATE
        );

        categorize(BlockCategory.RAIL,
                RAIL,
                GOLDEN_RAIL,
                ACTIVATOR_RAIL,
                DETECTOR_RAIL
        );


        inherit(BlockCategory.TRANSPARENT,
                BlockCategory.STAIRS,
                BlockCategory.SLAB,
                BlockCategory.FENCE,
                BlockCategory.FENCE_GATE,
                BlockCategory.WALLS,
                BlockCategory.CROPS,
                BlockCategory.DOOR,
                BlockCategory.TRAPDOOR,
                BlockCategory.SIGN,
                BlockCategory.STEM,
                BlockCategory.BUTTON,
                BlockCategory.PRESSURE_PLATE,
                BlockCategory.RAIL,
                BlockCategory.LIQUID
        );
    }

    public void inherit(BlockCategory parent, BlockCategory... children) {
        val parentSet = categoryMap.get(parent);

        for (BlockCategory child : children) {
            parentSet.addAll(categoryMap.get(child));
        }
    }

    public void categorize(BlockCategory category, Set<BlockType> types) {
        categoryMap.get(category).addAll(types);
    }

    public void categorize(BlockCategory category, BlockType... types) {
        categoryMap.get(category).addAll(Arrays.asList(types));
    }

    public boolean inCategory(BlockType type, BlockCategory category) {
        return categoryMap.get(category).contains(type);
    }

    public boolean inCategories(BlockType type, BlockCategory... categories) {
        for (BlockCategory category : categories) {
            if (!inCategory(type, category)) {
                return false;
            }
        }

        return true;
    }
}
