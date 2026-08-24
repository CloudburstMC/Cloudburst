package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;

/**
 * Registry keys for built-in item tags.
 */
@UtilityClass
public class ItemTags {

    /**
     * Items that repair wooden-tier tools.
     */
    public static final ItemTagKey WOODEN_TOOL_MATERIALS = tag("wooden_tool_materials");

    /**
     * Items that repair stone-tier tools.
     */
    public static final ItemTagKey STONE_TOOL_MATERIALS = tag("stone_tool_materials");

    /**
     * Items that repair copper-tier tools.
     */
    public static final ItemTagKey COPPER_TOOL_MATERIALS = tag("copper_tool_materials");

    /**
     * Items that repair iron-tier tools.
     */
    public static final ItemTagKey IRON_TOOL_MATERIALS = tag("iron_tool_materials");

    /**
     * Items that repair gold-tier tools.
     */
    public static final ItemTagKey GOLD_TOOL_MATERIALS = tag("gold_tool_materials");

    /**
     * Items that repair diamond-tier tools.
     */
    public static final ItemTagKey DIAMOND_TOOL_MATERIALS = tag("diamond_tool_materials");

    /**
     * Items that repair netherite-tier tools.
     */
    public static final ItemTagKey NETHERITE_TOOL_MATERIALS = tag("netherite_tool_materials");

    /**
     * Items that repair leather armor.
     */
    public static final ItemTagKey REPAIRS_LEATHER_ARMOR = tag("repairs_leather_armor");

    /**
     * Items that repair copper armor.
     */
    public static final ItemTagKey REPAIRS_COPPER_ARMOR = tag("repairs_copper_armor");

    /**
     * Items that repair chain armor.
     */
    public static final ItemTagKey REPAIRS_CHAIN_ARMOR = tag("repairs_chain_armor");

    /**
     * Items that repair iron armor.
     */
    public static final ItemTagKey REPAIRS_IRON_ARMOR = tag("repairs_iron_armor");

    /**
     * Items that repair gold armor.
     */
    public static final ItemTagKey REPAIRS_GOLD_ARMOR = tag("repairs_gold_armor");

    /**
     * Items that repair diamond armor.
     */
    public static final ItemTagKey REPAIRS_DIAMOND_ARMOR = tag("repairs_diamond_armor");

    /**
     * Items that repair netherite armor.
     */
    public static final ItemTagKey REPAIRS_NETHERITE_ARMOR = tag("repairs_netherite_armor");

    /**
     * Items that repair turtle helmets.
     */
    public static final ItemTagKey REPAIRS_TURTLE_HELMET = tag("repairs_turtle_helmet");

    /**
     * Items that repair wolf armor.
     */
    public static final ItemTagKey REPAIRS_WOLF_ARMOR = tag("repairs_wolf_armor");

    private static ItemTagKey tag(String path) {
        return ItemTagKey.of("minecraft:" + path);
    }
}
