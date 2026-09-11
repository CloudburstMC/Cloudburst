package org.cloudburstmc.api.item;

import net.kyori.adventure.text.format.TextColor;

/**
 * The named text colors, including the standard palette and the material-specific
 * colors used for armor trim overlays.
 *
 * <p>Each constant carries both an RGB value (via {@link TextColor}) and a
 * legacy section-code string (e.g. {@code §i}).</p>
 */
public final class MinecraftTextColor implements TextColor {

    public static final MinecraftTextColor BLACK = new MinecraftTextColor("black", '0', 0x000000);
    public static final MinecraftTextColor DARK_BLUE = new MinecraftTextColor("dark_blue", '1', 0x0000AA);
    public static final MinecraftTextColor DARK_GREEN = new MinecraftTextColor("dark_green", '2', 0x00AA00);
    public static final MinecraftTextColor DARK_AQUA = new MinecraftTextColor("dark_aqua", '3', 0x00AAAA);
    public static final MinecraftTextColor DARK_RED = new MinecraftTextColor("dark_red", '4', 0xAA0000);
    public static final MinecraftTextColor DARK_PURPLE = new MinecraftTextColor("dark_purple", '5', 0xAA00AA);
    public static final MinecraftTextColor GOLD = new MinecraftTextColor("gold", '6', 0xFFAA00);
    public static final MinecraftTextColor GRAY = new MinecraftTextColor("gray", '7', 0xAAAAAA);
    public static final MinecraftTextColor DARK_GRAY = new MinecraftTextColor("dark_gray", '8', 0x555555);
    public static final MinecraftTextColor BLUE = new MinecraftTextColor("blue", '9', 0x5555FF);
    public static final MinecraftTextColor GREEN = new MinecraftTextColor("green", 'a', 0x55FF55);
    public static final MinecraftTextColor AQUA = new MinecraftTextColor("aqua", 'b', 0x55FFFF);
    public static final MinecraftTextColor RED = new MinecraftTextColor("red", 'c', 0xFF5555);
    public static final MinecraftTextColor LIGHT_PURPLE = new MinecraftTextColor("light_purple", 'd', 0xFF55FF);
    public static final MinecraftTextColor YELLOW = new MinecraftTextColor("yellow", 'e', 0xFFFF55);
    public static final MinecraftTextColor WHITE = new MinecraftTextColor("white", 'f', 0xFFFFFF);
    public static final MinecraftTextColor MINECOIN_GOLD = new MinecraftTextColor("minecoin_gold", 'g', 0xDDD605);
    public static final MinecraftTextColor MATERIAL_QUARTZ = new MinecraftTextColor("material_quartz", 'h', 0xE3D4D1);
    public static final MinecraftTextColor MATERIAL_IRON = new MinecraftTextColor("material_iron", 'i', 0xCECACA);
    public static final MinecraftTextColor MATERIAL_NETHERITE = new MinecraftTextColor("material_netherite", 'j', 0x443A3B);
    public static final MinecraftTextColor MATERIAL_REDSTONE = new MinecraftTextColor("material_redstone", 'm', 0x971607);
    public static final MinecraftTextColor MATERIAL_COPPER = new MinecraftTextColor("material_copper", 'n', 0xB4684D);
    public static final MinecraftTextColor MATERIAL_GOLD = new MinecraftTextColor("material_gold", 'p', 0xDEB12D);
    public static final MinecraftTextColor MATERIAL_EMERALD = new MinecraftTextColor("material_emerald", 'q', 0x11A036);
    public static final MinecraftTextColor MATERIAL_DIAMOND = new MinecraftTextColor("material_diamond", 's', 0x2CBAA8);
    public static final MinecraftTextColor MATERIAL_LAPIS = new MinecraftTextColor("material_lapis", 't', 0x21497B);
    public static final MinecraftTextColor MATERIAL_AMETHYST = new MinecraftTextColor("material_amethyst", 'u', 0x9A5CC6);
    public static final MinecraftTextColor MATERIAL_RESIN = new MinecraftTextColor("material_resin", 'v', 0xEB7114);

    private final String name;
    private final String legacyCode;
    private final int value;

    private MinecraftTextColor(String name, char code, int value) {
        this.name = name;
        this.legacyCode = "§" + code;
        this.value = value;
    }

    public String name() {
        return this.name;
    }

    /**
     * The section-code string for this color, e.g. {@code §i}.
     */
    public String legacyCode() {
        return this.legacyCode;
    }

    @Override
    public int value() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
