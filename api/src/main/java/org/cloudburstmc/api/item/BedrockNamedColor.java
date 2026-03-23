package org.cloudburstmc.api.item;

import net.kyori.adventure.text.format.TextColor;

/**
 * The named text colors, including the standard palette and the material-specific
 * colors used for armor trim overlays.
 *
 * <p>Each constant carries both an RGB value (via {@link TextColor}) and a
 * legacy section-code string (e.g. {@code §i}).</p>
 */
public final class BedrockNamedColor implements TextColor {

    public static final BedrockNamedColor BLACK = new BedrockNamedColor("black", '0', 0x000000);
    public static final BedrockNamedColor DARK_BLUE = new BedrockNamedColor("dark_blue", '1', 0x0000AA);
    public static final BedrockNamedColor DARK_GREEN = new BedrockNamedColor("dark_green", '2', 0x00AA00);
    public static final BedrockNamedColor DARK_AQUA = new BedrockNamedColor("dark_aqua", '3', 0x00AAAA);
    public static final BedrockNamedColor DARK_RED = new BedrockNamedColor("dark_red", '4', 0xAA0000);
    public static final BedrockNamedColor DARK_PURPLE = new BedrockNamedColor("dark_purple", '5', 0xAA00AA);
    public static final BedrockNamedColor GOLD = new BedrockNamedColor("gold", '6', 0xFFAA00);
    public static final BedrockNamedColor GRAY = new BedrockNamedColor("gray", '7', 0xAAAAAA);
    public static final BedrockNamedColor DARK_GRAY = new BedrockNamedColor("dark_gray", '8', 0x555555);
    public static final BedrockNamedColor BLUE = new BedrockNamedColor("blue", '9', 0x5555FF);
    public static final BedrockNamedColor GREEN = new BedrockNamedColor("green", 'a', 0x55FF55);
    public static final BedrockNamedColor AQUA = new BedrockNamedColor("aqua", 'b', 0x55FFFF);
    public static final BedrockNamedColor RED = new BedrockNamedColor("red", 'c', 0xFF5555);
    public static final BedrockNamedColor LIGHT_PURPLE = new BedrockNamedColor("light_purple", 'd', 0xFF55FF);
    public static final BedrockNamedColor YELLOW = new BedrockNamedColor("yellow", 'e', 0xFFFF55);
    public static final BedrockNamedColor WHITE = new BedrockNamedColor("white", 'f', 0xFFFFFF);
    public static final BedrockNamedColor MINECOIN_GOLD = new BedrockNamedColor("minecoin_gold", 'g', 0xDDD605);
    public static final BedrockNamedColor MATERIAL_QUARTZ = new BedrockNamedColor("material_quartz", 'h', 0xE3D4D1);
    public static final BedrockNamedColor MATERIAL_IRON = new BedrockNamedColor("material_iron", 'i', 0xCECACA);
    public static final BedrockNamedColor MATERIAL_NETHERITE = new BedrockNamedColor("material_netherite", 'j', 0x443A3B);
    public static final BedrockNamedColor MATERIAL_REDSTONE = new BedrockNamedColor("material_redstone", 'm', 0x971607);
    public static final BedrockNamedColor MATERIAL_COPPER = new BedrockNamedColor("material_copper", 'n', 0xB4684D);
    public static final BedrockNamedColor MATERIAL_GOLD = new BedrockNamedColor("material_gold", 'p', 0xDEB12D);
    public static final BedrockNamedColor MATERIAL_EMERALD = new BedrockNamedColor("material_emerald", 'q', 0x11A036);
    public static final BedrockNamedColor MATERIAL_DIAMOND = new BedrockNamedColor("material_diamond", 's', 0x2CBAA8);
    public static final BedrockNamedColor MATERIAL_LAPIS = new BedrockNamedColor("material_lapis", 't', 0x21497B);
    public static final BedrockNamedColor MATERIAL_AMETHYST = new BedrockNamedColor("material_amethyst", 'u', 0x9A5CC6);
    public static final BedrockNamedColor MATERIAL_RESIN = new BedrockNamedColor("material_resin", 'v', 0xEB7114);

    private final String name;
    private final String legacyCode;
    private final int value;

    private BedrockNamedColor(String name, char code, int value) {
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
