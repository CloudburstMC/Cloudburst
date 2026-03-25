package org.cloudburstmc.api.crafting;

/**
 * Identifies the crafting mechanic a recipe belongs to.
 */
public enum RecipeType {
    /**
     * A recipe with unordered ingredients placed anywhere in the crafting grid.
     */
    SHAPELESS,
    /**
     * A recipe where ingredients must be placed in a specific grid arrangement.
     */
    SHAPED,
    /**
     * A recipe processed by a cooking station: furnace, blast furnace, smoker, or campfire.
     */
    COOKING,
    /**
     * A recipe whose output is computed at runtime rather than being a fixed item, such as firework crafting.
     */
    COMPLEX,
    /**
     * A shapeless recipe that recolors a shulker box by combining it with a dye.
     */
    SHULKER_BOX,
    /**
     * A shapeless recipe used for chemistry-based crafting. Behaves identically to {@link #SHAPELESS}.
     */
    SHAPELESS_CHEMISTRY,
    /**
     * A shaped recipe used for chemistry-based crafting. Behaves identically to {@link #SHAPED}.
     */
    SHAPED_CHEMISTRY,
    /**
     * A smithing table recipe that transforms a base item into a new item using a template and an addition material.
     */
    SMITHING_TRANSFORM,
    /**
     * A smithing table recipe that applies a decorative trim pattern to a piece of armor.
     */
    SMITHING_TRIM,
    /**
     * A recipe processed by the stonecutter, producing one specific cut output from a stone-type input.
     */
    STONECUTTING,
    /**
     * A brewing stand recipe that combines a base potion with a reagent to produce a new potion.
     */
    POTION,
    /**
     * A brewing stand recipe that changes the container type of the potion, such as converting a regular potion to a splash potion.
     */
    POTION_CONTAINER
}
