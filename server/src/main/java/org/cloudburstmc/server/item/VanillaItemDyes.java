package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;

import java.util.Map;
import java.util.Set;

@UtilityClass
public class VanillaItemDyes {

    private static final Map<ItemType, Integer> COLORS = Map.ofEntries(
            Map.entry(ItemTypes.WHITE_DYE, 0xF0F0F0),
            Map.entry(ItemTypes.ORANGE_DYE, 0xF9801D),
            Map.entry(ItemTypes.MAGENTA_DYE, 0xC74EBD),
            Map.entry(ItemTypes.LIGHT_BLUE_DYE, 0x3AB3DA),
            Map.entry(ItemTypes.YELLOW_DYE, 0xFED83D),
            Map.entry(ItemTypes.LIME_DYE, 0x80C71F),
            Map.entry(ItemTypes.PINK_DYE, 0xF38BAA),
            Map.entry(ItemTypes.GRAY_DYE, 0x474F52),
            Map.entry(ItemTypes.LIGHT_GRAY_DYE, 0x9D9D97),
            Map.entry(ItemTypes.CYAN_DYE, 0x169C9C),
            Map.entry(ItemTypes.PURPLE_DYE, 0x8932B8),
            Map.entry(ItemTypes.BLUE_DYE, 0x3C44AA),
            Map.entry(ItemTypes.BROWN_DYE, 0x835432),
            Map.entry(ItemTypes.GREEN_DYE, 0x5E7C16),
            Map.entry(ItemTypes.RED_DYE, 0xB02E26),
            Map.entry(ItemTypes.BLACK_DYE, 0x1D1D21));

    private static final Set<Identifier> DYEABLE = Set.of(
            ItemTypes.LEATHER_HELMET.getId(), ItemTypes.LEATHER_CHESTPLATE.getId(),
            ItemTypes.LEATHER_LEGGINGS.getId(), ItemTypes.LEATHER_BOOTS.getId(),
            ItemTypes.LEATHER_HORSE_ARMOR.getId(), ItemTypes.WOLF_ARMOR.getId());

    public static @Nullable Integer color(ItemType type) {
        return COLORS.get(type);
    }

    public static boolean isDyeable(Identifier id) {
        return DYEABLE.contains(id);
    }

    public static int mix(int first, int second) {
        int red = mixChannel(first >>> 16 & 255, second >>> 16 & 255);
        int green = mixChannel(first >>> 8 & 255, second >>> 8 & 255);
        int blue = mixChannel(first & 255, second & 255);
        return red << 16 | green << 8 | blue;
    }

    private static int mixChannel(int first, int second) {
        return (int) Math.round(Math.sqrt(first * second) * 0.965);
    }
}
