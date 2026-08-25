package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;

/**
 * Registry keys for built-in block tags.
 */
@UtilityClass
public class BlockTags {
    /**
     * Blocks that are beds.
     */
    public static final BlockTagKey BEDS = tag("beds");

    /**
     * Blocks that are buttons.
     */
    public static final BlockTagKey BUTTON = tag("button");

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
     * Blocks that are fences.
     */
    public static final BlockTagKey FENCE = tag("fence");

    /**
     * Blocks that are fence gates.
     */
    public static final BlockTagKey FENCE_GATE = tag("fence_gate");

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
     * Blocks that are liquids.
     */
    public static final BlockTagKey LIQUID = tag("liquid");

    /**
     * Blocks that are logs or stems.
     */
    public static final BlockTagKey LOG = tag("log");

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
     * Blocks treated as transparent for vanilla visibility or rendering checks.
     */
    public static final BlockTagKey TRANSPARENT = tag("transparent");

    /**
     * Blocks that are trapdoors.
     */
    public static final BlockTagKey TRAPDOOR = tag("trapdoor");

    /**
     * Blocks with unstable bottom-center support.
     */
    public static final BlockTagKey UNSTABLE_BOTTOM_CENTER = tag("unstable_bottom_center");

    /**
     * Blocks that are walls.
     */
    public static final BlockTagKey WALLS = tag("walls");

    /**
     * Blocks that are wool.
     */
    public static final BlockTagKey WOOL = tag("wool");

    private static BlockTagKey tag(String path) {
        return BlockTagKey.of("minecraft:" + path);
    }
}
