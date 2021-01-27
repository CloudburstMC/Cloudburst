package org.cloudburstmc.api.util.data;

public enum BlockColor {

    TRANSPARENT_BLOCK_COLOR(0x00, 0x00, 0x00, 0x00),
    VOID_BLOCK_COLOR(0x00, 0x00, 0x00, 0x00),

    AIR_BLOCK_COLOR(0x00, 0x00, 0x00),
    GRASS_BLOCK_COLOR(0x7f, 0xb2, 0x38),
    SAND_BLOCK_COLOR(0xf1, 0xe9, 0xa3),
    CLOTH_BLOCK_COLOR(0xa7, 0xa7, 0xa7),
    TNT_BLOCK_COLOR(0xff, 0x00, 0x00),
    ICE_BLOCK_COLOR(0xa0, 0xa0, 0xff),
    IRON_BLOCK_COLOR(0xa7, 0xa7, 0xa7),
    FOLIAGE_BLOCK_COLOR(0x00, 0x7c, 0x00),
    SNOW_BLOCK_COLOR(0xff, 0xff, 0xff),
    CLAY_BLOCK_COLOR(0xa4, 0xa8, 0xb8),
    DIRT_BLOCK_COLOR(0xb7, 0x6a, 0x2f),
    STONE_BLOCK_COLOR(0x70, 0x70, 0x70),
    WATER_BLOCK_COLOR(0x40, 0x40, 0xff),
    LAVA_BLOCK_COLOR(0xff, 0x00, 0x00),
    WOOD_BLOCK_COLOR(0x68, 0x53, 0x32),
    QUARTZ_BLOCK_COLOR(0xff, 0xfc, 0xf5),
    ADOBE_BLOCK_COLOR(0xd8, 0x7f, 0x33),

    WHITE_BLOCK_COLOR(0xff, 0xff, 0xff),
    ORANGE_BLOCK_COLOR(0xd8, 0x7f, 0x33),
    MAGENTA_BLOCK_COLOR(0xb2, 0x4c, 0xd8),
    LIGHT_BLUE_BLOCK_COLOR(0x66, 0x99, 0xd8),
    YELLOW_BLOCK_COLOR(0xe5, 0xe5, 0x33),
    LIME_BLOCK_COLOR(0x7f, 0xcc, 0x19),
    PINK_BLOCK_COLOR(0xf2, 0x7f, 0xa5),
    GRAY_BLOCK_COLOR(0x4c, 0x4c, 0x4c),
    LIGHT_GRAY_BLOCK_COLOR(0x99, 0x99, 0x99),
    CYAN_BLOCK_COLOR(0x4c, 0x7f, 0x99),
    PURPLE_BLOCK_COLOR(0x7f, 0x3f, 0xb2),
    BLUE_BLOCK_COLOR(0x33, 0x4c, 0xb2),
    BROWN_BLOCK_COLOR(0x66, 0x4c, 0x33),
    GREEN_BLOCK_COLOR(0x66, 0x7f, 0x33),
    RED_BLOCK_COLOR(0x99, 0x33, 0x33),
    BLACK_BLOCK_COLOR(0x19, 0x19, 0x19),

    GOLD_BLOCK_COLOR(0xfa, 0xee, 0x4d),
    DIAMOND_BLOCK_COLOR(0x5c, 0xdb, 0xd5),
    LAPIS_BLOCK_COLOR(0x4a, 0x80, 0xff),
    EMERALD_BLOCK_COLOR(0x00, 0xd9, 0x3a),
    OBSIDIAN_BLOCK_COLOR(0x15, 0x14, 0x1f),
    SPRUCE_BLOCK_COLOR(0x81, 0x56, 0x31),
    NETHERRACK_BLOCK_COLOR(0x70, 0x02, 0x00),
    REDSTONE_BLOCK_COLOR(0xff, 0x00, 0x00),

    WHITE_TERRACOTA_BLOCK_COLOR(0xd1, 0xb1, 0xa1),
    ORANGE_TERRACOTA_BLOCK_COLOR(0x9f, 0x52, 0x24),
    MAGENTA_TERRACOTA_BLOCK_COLOR(0x95, 0x57, 0x6c),
    LIGHT_BLUE_TERRACOTA_BLOCK_COLOR(0x70, 0x6c, 0x8a),
    YELLOW_TERRACOTA_BLOCK_COLOR(0xba, 0x85, 0x24),
    LIME_TERRACOTA_BLOCK_COLOR(0x67, 0x75, 0x35),
    PINK_TERRACOTA_BLOCK_COLOR(0xa0, 0x4d, 0x4e),
    GRAY_TERRACOTA_BLOCK_COLOR(0x39, 0x29, 0x23),
    LIGHT_GRAY_TERRACOTA_BLOCK_COLOR(0x87, 0x6b, 0x62),
    CYAN_TERRACOTA_BLOCK_COLOR(0x57, 0x5c, 0x5c),
    PURPLE_TERRACOTA_BLOCK_COLOR(0x7a, 0x49, 0x58),
    BLUE_TERRACOTA_BLOCK_COLOR(0x4c, 0x3e, 0x5c),
    BROWN_TERRACOTA_BLOCK_COLOR(0x4c, 0x32, 0x23),
    GREEN_TERRACOTA_BLOCK_COLOR(0x4c, 0x52, 0x2a),
    RED_TERRACOTA_BLOCK_COLOR(0x8e, 0x3c, 0x2e),
    BLACK_TERRACOTA_BLOCK_COLOR(0x25, 0xb16, 0x10);


    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;

    BlockColor(int red, int green, int blue) {
        this(red, green, blue, 0xff);
    }

    BlockColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    BlockColor(int rgb) {
        this.red = (rgb >> 16) & 0xff;
        this.green = (rgb >> 8) & 0xff;
        this.blue = rgb & 0xff;
        this.alpha = 0xff;
    }

    public int getRed() {
        return this.red;
    }

    public int getGreen() {
        return this.green;
    }

    public int getBlue() {
        return this.blue;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public int getRGB() {
        return (this.red << 16 | this.green << 8 | this.blue) & 0xffffff;
    }

    @Override
    public String toString() {
        return "BlockColor[r=" + this.red + ",g=" + this.green + ",b=" + this.blue + ",a=" + this.alpha + "]";
    }
}
