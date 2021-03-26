package org.cloudburstmc.api.inventory;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public enum InventoryType {
    CHEST(27, "Chest"),
    ENDER_CHEST(27, "Ender Chest"),
    DOUBLE_CHEST(27 + 27, "Double Chest"),
    PLAYER(41, "Player"), //36 CONTAINER, 4 ARMOR, 1 OFFHAND
    FURNACE(3, "Furnace"),
    CRAFTING(5, "Crafting"), //4 CRAFTING slots, 1 RESULT
    WORKBENCH(10, "Crafting"), //9 CRAFTING slots, 1 RESULT
    BREWING_STAND(5, "Brewing"), //1 INPUT, 3 POTION, 1 fuel
    ANVIL(3, "Anvil"), //2 INPUT, 1 OUTPUT
    ENCHANT_TABLE(2, "Enchantment Table"), //1 INPUT/OUTPUT, 1 LAPIS
    DISPENSER(9, "Dispenser"), //9 CONTAINER
    DROPPER(9, "Dropper"), //9 CONTAINER
    HOPPER(5, "Hopper"), //5 CONTAINER
    UI(1, "UI"),
    SHULKER_BOX(27, "Shulker Box"),
    BEACON(1, "Beacon"),
    BLAST_FURNACE(3, "Blast Furnace"),
    SMOKER(3, "Smoker"),
    BARREL(27, "Barrel"),
    LOOM(4, "Loom"),
    STONECUTTER(2, "Stonecutter"),
    GRINDSTONE(3, "Repair & Disenchant"),
    SMITHING_TABLE(3, "Upgrade Gear"),
    CARTOGRAPHY_TABLE(3, "Cartography Table");

    private final int size;
    private final String title;

    InventoryType(int defaultSize, String defaultTitle) {
        this.size = defaultSize;
        this.title = defaultTitle;
    }

    public int getDefaultSize() {
        return size;
    }

    public String getDefaultTitle() {
        return title;
    }

}
