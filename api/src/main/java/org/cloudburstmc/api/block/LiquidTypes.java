package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Vanilla liquid types.
 */
@UtilityClass
public class LiquidTypes {

    private static final Identifier EMPTY_FAMILY = Identifier.parse("empty");
    private static final Identifier WATER_FAMILY = Identifier.parse("water");
    private static final Identifier LAVA_FAMILY = Identifier.parse("lava");

    public static final LiquidType EMPTY = type("empty", EMPTY_FAMILY);
    public static final LiquidType FLOWING_WATER = type("flowing_water", WATER_FAMILY);
    public static final LiquidType WATER = type("water", WATER_FAMILY);
    public static final LiquidType FLOWING_LAVA = type("flowing_lava", LAVA_FAMILY);
    public static final LiquidType LAVA = type("lava", LAVA_FAMILY);

    private static LiquidType type(String id, Identifier family) {
        return new LiquidType(Identifier.parse(id), family);
    }
}
