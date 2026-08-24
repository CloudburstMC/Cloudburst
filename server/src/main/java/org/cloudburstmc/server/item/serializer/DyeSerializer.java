package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemTypes;
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
            case BLACK -> ItemTypes.INK_SAC.getId();
            case RED -> ItemTypes.RED_DYE.getId();
            case GREEN -> ItemTypes.GREEN_DYE.getId();
            case BROWN -> ItemTypes.COCOA_BEANS.getId();
            case BLUE -> ItemTypes.LAPIS_LAZULI.getId();
            case PURPLE -> ItemTypes.PURPLE_DYE.getId();
            case CYAN -> ItemTypes.CYAN_DYE.getId();
            case LIGHT_GRAY -> ItemTypes.LIGHT_GRAY_DYE.getId();
            case GRAY -> ItemTypes.GRAY_DYE.getId();
            case PINK -> ItemTypes.PINK_DYE.getId();
            case LIME -> ItemTypes.LIME_DYE.getId();
            case YELLOW -> ItemTypes.YELLOW_DYE.getId();
            case LIGHT_BLUE -> ItemTypes.LIGHT_BLUE_DYE.getId();
            case MAGENTA -> ItemTypes.MAGENTA_DYE.getId();
            case ORANGE -> ItemTypes.ORANGE_DYE.getId();
            default -> ItemTypes.BONE_MEAL.getId();
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        if (ItemTypes.INK_SAC.getId().equals(id) || ItemTypes.BLACK_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.BLACK);
            return;
        }

        if (ItemTypes.RED_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.RED);
            return;
        }

        if (ItemTypes.GREEN_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.GREEN);
            return;
        }
        if (ItemTypes.COCOA_BEANS.getId().equals(id) || ItemTypes.BROWN_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.BROWN);
            return;
        }
        if (ItemTypes.LAPIS_LAZULI.getId().equals(id) || ItemTypes.BLUE_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.BLUE);
            return;
        }
        if (ItemTypes.PURPLE_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.PURPLE);
            return;
        }
        if (ItemTypes.CYAN_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.CYAN);
            return;
        }
        if (ItemTypes.LIGHT_GRAY_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.LIGHT_GRAY);
            return;
        }
        if (ItemTypes.GRAY_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.GRAY);
            return;
        }
        if (ItemTypes.PINK_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.PINK);
            return;
        }
        if (ItemTypes.LIME_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.LIME);
            return;
        }
        if (ItemTypes.YELLOW_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.YELLOW);
            return;
        }
        if (ItemTypes.LIGHT_BLUE_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.LIGHT_BLUE);
            return;
        }
        if (ItemTypes.MAGENTA_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.MAGENTA);
            return;
        }
        if (ItemTypes.ORANGE_DYE.getId().equals(id)) {
            builder.data(ItemKeys.COLOR, DyeColor.ORANGE);
            return;
        }
        if (ItemTypes.BONE_MEAL.getId().equals(id) || ItemTypes.WHITE_DYE.getId().equals(id)) {
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
