package org.cloudburstmc.api.util.data;

import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A colored pattern layer on a banner.
 */
@EqualsAndHashCode
public class BannerPattern {

    private final Type type;
    private final DyeColor color;

    /**
     * @param type  the pattern shape
     * @param color the pattern color
     */
    public BannerPattern(Type type, DyeColor color) {
        this.type = Objects.requireNonNull(type, "type");
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * @return the pattern color
     */
    public DyeColor getColor() {
        return this.color;
    }

    /**
     * @return the pattern shape
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Built-in banner pattern shapes.
     */
    public enum Type {

        PATTERN_BOTTOM_STRIPE("bs"),
        PATTERN_TOP_STRIPE("ts"),
        PATTERN_LEFT_STRIPE("ls"),
        PATTERN_RIGHT_STRIPE("rs"),
        PATTERN_CENTER_STRIPE("cs"),
        PATTERN_MIDDLE_STRIPE("ms"),
        PATTERN_DOWN_RIGHT_STRIPE("drs"),
        PATTERN_DOWN_LEFT_STRIPE("dls"),
        PATTERN_SMALL_STRIPES("ss"),
        PATTERN_DIAGONAL_CROSS("cr"),
        PATTERN_SQUARE_CROSS("sc"),
        PATTERN_LEFT_OF_DIAGONAL("ld"),
        PATTERN_RIGHT_OF_UPSIDE_DOWN_DIAGONAL("rud"),
        PATTERN_LEFT_OF_UPSIDE_DOWN_DIAGONAL("lud"),
        PATTERN_RIGHT_OF_DIAGONAL("rd"),
        PATTERN_VERTICAL_HALF_LEFT("vh"),
        PATTERN_VERTICAL_HALF_RIGHT("vhr"),
        PATTERN_HORIZONTAL_HALF_TOP("hh"),
        PATTERN_HORIZONTAL_HALF_BOTTOM("hhb"),
        PATTERN_BOTTOM_LEFT_CORNER("bl"),
        PATTERN_BOTTOM_RIGHT_CORNER("br"),
        PATTERN_TOP_LEFT_CORNER("tl"),
        PATTERN_TOP_RIGHT_CORNER("tr"),
        PATTERN_BOTTOM_TRIANGLE("bt"),
        PATTERN_TOP_TRIANGLE("tt"),
        PATTERN_BOTTOM_TRIANGLE_SAWTOOTH("bts"),
        PATTERN_TOP_TRIANGLE_SAWTOOTH("tts"),
        PATTERN_MIDDLE_CIRCLE("mc"),
        PATTERN_MIDDLE_RHOMBUS("mr"),
        PATTERN_BORDER("bo"),
        PATTERN_CURLY_BORDER("cbo"),
        PATTERN_BRICK("bri"),
        PATTERN_GRADIENT("gra"),
        PATTERN_GRADIENT_UPSIDE_DOWN("gru"),
        PATTERN_CREEPER("cre"),
        PATTERN_SKULL("sku"),
        PATTERN_FLOWER("flo"),
        PATTERN_MOJANG("moj"),
        PATTERN_GLOBE("glb"),
        PATTERN_PIGLIN("pig"),
        PATTERN_FLOW("flw"),
        PATTERN_GUSTER("gus");

        private static final Map<String, Type> BY_NAME = new HashMap<>();

        static {
            for (Type type : values()) {
                BY_NAME.put(type.getName(), type);
            }
        }

        private final String name;

        private Type(String name) {
            this.name = name;
        }

        /**
         * @param name the pattern name
         * @return the matching shape, or null when unknown
         */
        public static @Nullable Type getByName(String name) {
            return BY_NAME.get(name);
        }

        /**
         * @return the pattern name
         */
        public String getName() {
            return this.name;
        }

    }

}
