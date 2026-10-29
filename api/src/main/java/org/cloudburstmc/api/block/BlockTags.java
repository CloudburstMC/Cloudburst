package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;

/**
 * Registry keys for built-in block tags.
 */
@UtilityClass
public class BlockTags {

    /**
     * Bars that connect to other bars, glass panes, and walls.
     */
    public static final BlockTagKey BARS = tag("bars");

    /**
     * Blocks that are beds.
     */
    public static final BlockTagKey BEDS = tag("beds");

    /**
     * Blocks that are buttons.
     */
    public static final BlockTagKey BUTTON = tag("button");

    /**
     * Blocks that allow gliding without entering a climbing state.
     */
    public static final BlockTagKey CAN_GLIDE_THROUGH = tag("can_glide_through");

    /**
     * Blocks that cannot support kelp even when their upper face is sturdy.
     */
    public static final BlockTagKey CANNOT_SUPPORT_KELP = tag("cannot_support_kelp");

    /**
     * Blocks that cannot support seagrass even when their upper face is sturdy.
     */
    public static final BlockTagKey CANNOT_SUPPORT_SEAGRASS = tag("cannot_support_seagrass");

    /**
     * Blocks that living entities can climb.
     */
    public static final BlockTagKey CLIMBABLE = tag("climbable");

    /**
     * Blocks that count toward a conduit's frame.
     */
    public static final BlockTagKey CONDUIT_EFFECT_BLOCK = tag("conduit_effect_block");

    /**
     * Living coral blocks on which sea pickles can multiply when fertilized.
     */
    public static final BlockTagKey CORAL_BLOCKS = tag("coral_blocks");

    /**
     * Living coral plants and floor fans.
     */
    public static final BlockTagKey CORALS = tag("corals");

    /**
     * Blocks that are growable crop plants.
     */
    public static final BlockTagKey CROPS = tag("crops");

    /**
     * Blocks that are doors.
     */
    public static final BlockTagKey DOOR = tag("door");

    /**
     * Blocks that are double slabs.
     */
    public static final BlockTagKey DOUBLE_SLAB = tag("double_slab");

    /**
     * Blocks that create downward bubble columns in source water above them.
     */
    public static final BlockTagKey ENABLES_BUBBLE_COLUMN_DRAG_DOWN = tag("enables_bubble_column_drag_down");

    /**
     * Blocks that create upward bubble columns in source water above them.
     */
    public static final BlockTagKey ENABLES_BUBBLE_COLUMN_PUSH_UP = tag("enables_bubble_column_push_up");

    /**
     * Blocks that reset accumulated fall distance while an entity occupies them.
     */
    public static final BlockTagKey FALL_DAMAGE_RESETTING = tag("fall_damage_resetting");

    /**
     * Blocks that are fences.
     */
    public static final BlockTagKey FENCE = tag("fence");

    /**
     * Blocks that are fence gates.
     */
    public static final BlockTagKey FENCE_GATE = tag("fence_gate");

    /**
     * Glass panes that connect to bars, other panes, and walls.
     */
    public static final BlockTagKey GLASS_PANES = tag("glass_panes");

    /**
     * Ground blocks used by vanilla placement, support, or movement checks.
     */
    public static final BlockTagKey GROUND = tag("ground");

    /**
     * Blocks that cannot be correctly harvested by copper-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_COPPER_TOOL = tag("incorrect_for_copper_tool");

    /**
     * Blocks that cannot be correctly harvested by diamond-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_DIAMOND_TOOL = tag("incorrect_for_diamond_tool");

    /**
     * Blocks that cannot be correctly harvested by gold-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_GOLD_TOOL = tag("incorrect_for_gold_tool");

    /**
     * Blocks that cannot be correctly harvested by iron-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_IRON_TOOL = tag("incorrect_for_iron_tool");

    /**
     * Blocks that cannot be correctly harvested by netherite-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_NETHERITE_TOOL = tag("incorrect_for_netherite_tool");

    /**
     * Blocks that cannot be correctly harvested by stone-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_STONE_TOOL = tag("incorrect_for_stone_tool");

    /**
     * Blocks that cannot be correctly harvested by wooden-tier tools.
     */
    public static final BlockTagKey INCORRECT_FOR_WOODEN_TOOL = tag("incorrect_for_wooden_tool");

    /**
     * Blocks that are leaves.
     */
    public static final BlockTagKey LEAVES = tag("leaves");

    /**
     * Blocks that conduct a Channeling trident strike.
     */
    public static final BlockTagKey LIGHTNING_RODS = tag("lightning_rods");

    /**
     * Blocks that are liquids.
     */
    public static final BlockTagKey LIQUID = tag("liquid");

    /**
     * Blocks that are logs or stems.
     */
    public static final BlockTagKey LOG = tag("log");

    /**
     * Blocks that mangrove trunks may replace while growing.
     */
    public static final BlockTagKey MANGROVE_LOGS_CAN_GROW_THROUGH = tag("mangrove_logs_can_grow_through");

    /**
     * Blocks that mangrove roots may replace while growing.
     */
    public static final BlockTagKey MANGROVE_ROOTS_CAN_GROW_THROUGH = tag("mangrove_roots_can_grow_through");

    /**
     * Blocks whose heat melts icicles one or two blocks above them.
     */
    public static final BlockTagKey MELTS_ICICLE_ABOVE = tag("melts_icicle_above");

    /**
     * Blocks mined most effectively with axes.
     */
    public static final BlockTagKey MINEABLE_WITH_AXE = tag("mineable/axe");

    /**
     * Blocks mined most effectively with hoes.
     */
    public static final BlockTagKey MINEABLE_WITH_HOE = tag("mineable/hoe");

    /**
     * Blocks mined most effectively with pickaxes.
     */
    public static final BlockTagKey MINEABLE_WITH_PICKAXE = tag("mineable/pickaxe");

    /**
     * Blocks mined most effectively with shovels.
     */
    public static final BlockTagKey MINEABLE_WITH_SHOVEL = tag("mineable/shovel");

    /**
     * Blocks that require at least diamond-tier tools to drop correctly.
     */
    public static final BlockTagKey NEEDS_DIAMOND_TOOL = tag("needs_diamond_tool");

    /**
     * Blocks that require at least iron-tier tools to drop correctly.
     */
    public static final BlockTagKey NEEDS_IRON_TOOL = tag("needs_iron_tool");

    /**
     * Blocks that require at least stone-tier tools to drop correctly.
     */
    public static final BlockTagKey NEEDS_STONE_TOOL = tag("needs_stone_tool");

    /**
     * Blocks that are planks.
     */
    public static final BlockTagKey PLANKS = tag("planks");

    /**
     * Blocks that are pressure plates.
     */
    public static final BlockTagKey PRESSURE_PLATE = tag("pressure_plate");

    /**
     * Blocks that are rails.
     */
    public static final BlockTagKey RAIL = tag("rail");

    /**
     * Blocks that may be replaced by block placement.
     */
    public static final BlockTagKey REPLACEABLE = tag("replaceable");

    /**
     * Blocks that may be replaced by growing trees.
     */
    public static final BlockTagKey REPLACEABLE_BY_TREES = tag("replaceable_by_trees");

    /**
     * Sand, red sand, and suspicious sand.
     */
    public static final BlockTagKey SAND = tag("sand");

    /**
     * Blocks that are saplings.
     */
    public static final BlockTagKey SAPLING = tag("sapling");

    /**
     * Blocks that shears break at their highest mining speed.
     */
    public static final BlockTagKey SHEARS_EXTREME_BREAKING_SPEED = tag("shears_extreme_breaking_speed");

    /**
     * Blocks that shears break at their primary mining speed.
     */
    public static final BlockTagKey SHEARS_MAJOR_BREAKING_SPEED = tag("shears_major_breaking_speed");

    /**
     * Blocks that shears break faster than hand mining.
     */
    public static final BlockTagKey SHEARS_MINOR_BREAKING_SPEED = tag("shears_minor_breaking_speed");

    /**
     * Blocks that are shulker boxes.
     */
    public static final BlockTagKey SHULKER_BOXES = tag("shulker_boxes");

    /**
     * Blocks that are signs.
     */
    public static final BlockTagKey SIGN = tag("sign");

    /**
     * Blocks that are slabs.
     */
    public static final BlockTagKey SLAB = tag("slab");

    /**
     * Blocks treated as solid for vanilla support and collision checks.
     */
    public static final BlockTagKey SOLID = tag("solid");

    /**
     * Vegetation that can share a position with a snow layer.
     */
    public static final BlockTagKey SNOWLOGGABLE = tag("snowloggable");

    /**
     * Pointed dripstone, sulfur spikes and icicles.
     */
    public static final BlockTagKey SPELEOTHEMS = tag("speleothems");

    /**
     * Blocks that swords break faster than hand mining.
     */
    public static final BlockTagKey SWORD_EFFICIENT = tag("sword_efficient");

    /**
     * Blocks that swords break instantly.
     */
    public static final BlockTagKey SWORD_INSTANTLY_MINES = tag("sword_instantly_mines");

    /**
     * Blocks that are stairs.
     */
    public static final BlockTagKey STAIRS = tag("stairs");

    /**
     * Blocks that are mushroom stems or plant stems.
     */
    public static final BlockTagKey STEM = tag("stem");

    /**
     * Ground blocks that support a big dripleaf or its stem.
     */
    public static final BlockTagKey SUPPORTS_BIG_DRIPLEAF = tag("supports_big_dripleaf");

    /**
     * Blocks on which chorus flowers may survive.
     */
    public static final BlockTagKey SUPPORTS_CHORUS_FLOWER = tag("supports_chorus_flower");

    /**
     * Blocks to which chorus plants may connect.
     */
    public static final BlockTagKey SUPPORTS_CHORUS_PLANT = tag("supports_chorus_plant");

    /**
     * Blocks on which dry vegetation may survive.
     */
    public static final BlockTagKey SUPPORTS_DRY_VEGETATION = tag("supports_dry_vegetation");

    /**
     * Blocks from which hanging mangrove propagules may grow.
     */
    public static final BlockTagKey SUPPORTS_HANGING_MANGROVE_PROPAGULE = tag("supports_hanging_mangrove_propagule");

    /**
     * Solid blocks that support lily pads without water underneath.
     */
    public static final BlockTagKey SUPPORTS_LILY_PAD = tag("supports_lily_pad");

    /**
     * Ground blocks on which mangrove propagules may survive.
     */
    public static final BlockTagKey SUPPORTS_MANGROVE_PROPAGULE = tag("supports_mangrove_propagule");

    /**
     * Ground blocks on which small dripleaf may be planted without water.
     */
    public static final BlockTagKey SUPPORTS_SMALL_DRIPLEAF = tag("supports_small_dripleaf");

    /**
     * Ground blocks that support sugar cane when water or another valid adjacent support is present.
     */
    public static final BlockTagKey SUPPORTS_SUGAR_CANE = tag("supports_sugar_cane");

    /**
     * Blocks that can replace adjacent water in sugar cane's survival check.
     */
    public static final BlockTagKey SUPPORTS_SUGAR_CANE_ADJACENTLY = tag("supports_sugar_cane_adjacently");

    /**
     * Blocks on which ordinary vegetation may survive.
     */
    public static final BlockTagKey SUPPORTS_VEGETATION = tag("supports_vegetation");

    /**
     * Blocks on which wither roses may survive.
     */
    public static final BlockTagKey SUPPORTS_WITHER_ROSE = tag("supports_wither_rose");

    /**
     * Blocks treated as transparent for vanilla visibility or rendering checks.
     */
    public static final BlockTagKey TRANSPARENT = tag("transparent");

    /**
     * Blocks that are trapdoors.
     */
    public static final BlockTagKey TRAPDOOR = tag("trapdoor");

    /**
     * Plants that bone meal can grow in water.
     */
    public static final BlockTagKey UNDERWATER_BONEMEALS = tag("underwater_bonemeals");

    /**
     * Blocks with unstable bottom-center support.
     */
    public static final BlockTagKey UNSTABLE_BOTTOM_CENTER = tag("unstable_bottom_center");

    /**
     * Blocks that force a wall below them to keep its center post.
     */
    public static final BlockTagKey WALL_POST_OVERRIDE = tag("wall_post_override");

    /**
     * Blocks that are walls.
     */
    public static final BlockTagKey WALLS = tag("walls");

    /**
     * Living coral fans attached to a wall.
     */
    public static final BlockTagKey WALL_CORALS = tag("wall_corals");

    /**
     * Plants whose block space always contains source water, rather than optionally containing liquid.
     */
    public static final BlockTagKey WATER_SOURCE_PLANTS = tag("water_source_plants");

    /**
     * Fences that connect to other wooden fences.
     */
    public static final BlockTagKey WOODEN_FENCE = tag("wooden_fence");

    /**
     * Blocks that are wool.
     */
    public static final BlockTagKey WOOL = tag("wool");

    private static BlockTagKey tag(String path) {
        return BlockTagKey.of("minecraft:" + path);
    }
}
