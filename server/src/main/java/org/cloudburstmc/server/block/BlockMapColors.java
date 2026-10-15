package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;

import java.awt.*;

@UtilityClass
public class BlockMapColors {

    private static final Color GRASS = new Color(0x7fb238);
    private static final Color PLANT = new Color(0x007c00);
    private static final Color WATER = new Color(0x4040ff);

    public static Color resolve(String value, String tintMethod, BlockType type) {
        if (value.length() != 9 || value.charAt(0) != '#') {
            throw new IllegalArgumentException("Invalid map color: " + value);
        }

        Color color = new Color(Integer.parseInt(value.substring(1, 3), 16),
                Integer.parseInt(value.substring(3, 5), 16),
                Integer.parseInt(value.substring(5, 7), 16),
                Integer.parseInt(value.substring(7, 9), 16));
        if (color.getAlpha() != 0) {
            return color;
        }

        return switch (tintMethod) {
            case "Water" -> WATER;
            case "Grass" -> type == BlockTypes.GRASS_BLOCK ? GRASS : PLANT;
            case "DefaultFoliage", "BirchFoliage", "EvergreenFoliage", "Stem" -> PLANT;
            default -> color;
        };
    }
}
