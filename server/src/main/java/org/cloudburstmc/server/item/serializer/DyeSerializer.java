package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class DyeSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(DyeColor.class, DyeColor.WHITE);
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Identifier id;
        DyeColor color = item.get(ItemKeys.COLOR);

        id = switch (color) {
            case BLACK -> ItemIds.INK_SAC;
            case RED -> ItemIds.RED_DYE;
            case GREEN -> ItemIds.GREEN_DYE;
            case BROWN -> ItemIds.COCOA_BEANS;
            case BLUE -> ItemIds.LAPIS_LAZULI;
            case PURPLE -> ItemIds.PURPLE_DYE;
            case CYAN -> ItemIds.CYAN_DYE;
            case LIGHT_GRAY -> ItemIds.LIGHT_GRAY_DYE;
            case GRAY -> ItemIds.GRAY_DYE;
            case PINK -> ItemIds.PINK_DYE;
            case LIME -> ItemIds.LIME_DYE;
            case YELLOW -> ItemIds.YELLOW_DYE;
            case LIGHT_BLUE -> ItemIds.LIGHT_BLUE_DYE;
            case MAGENTA -> ItemIds.MAGENTA_DYE;
            case ORANGE -> ItemIds.ORANGE_DYE;
            default -> ItemIds.BONE_MEAL;
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        if (id == ItemIds.INK_SAC || id == ItemIds.BLACK_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.BLACK);
            return;
        }

        if (id == ItemIds.RED_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.RED);
            return;
        }

        if (id == ItemIds.GREEN_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.GREEN);
            return;
        }
        if (id == ItemIds.COCOA_BEANS || id == ItemIds.BROWN_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.BROWN);
            return;
        }
        if (id == ItemIds.LAPIS_LAZULI || id == ItemIds.BLUE_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.BLUE);
            return;
        }
        if (id == ItemIds.PURPLE_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.PURPLE);
            return;
        }
        if (id == ItemIds.CYAN_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.CYAN);
            return;
        }
        if (id == ItemIds.LIGHT_GRAY_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.LIGHT_GRAY);
            return;
        }
        if (id == ItemIds.GRAY_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.GRAY);
            return;
        }
        if (id == ItemIds.PINK_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.PINK);
            return;
        }
        if (id == ItemIds.LIME_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.LIME);
            return;
        }
        if (id == ItemIds.YELLOW_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.YELLOW);
            return;
        }
        if (id == ItemIds.LIGHT_BLUE_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.LIGHT_BLUE);
            return;
        }
        if (id == ItemIds.MAGENTA_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.MAGENTA);
            return;
        }
        if (id == ItemIds.ORANGE_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.ORANGE);
            return;
        }
        if (id == ItemIds.BONE_MEAL || id == ItemIds.WHITE_DYE) {
            builder.data(ItemKeys.COLOR, DyeColor.WHITE);
            return;
        }

        builder.data(ItemKeys.COLOR, DyeColor.BLACK);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
