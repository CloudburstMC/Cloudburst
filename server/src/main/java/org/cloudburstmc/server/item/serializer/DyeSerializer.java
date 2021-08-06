package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;

import java.util.Map;

public class DyeSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(DyeColor.class, DyeColor.WHITE);
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Identifier id;
        DyeColor color = item.getMetadata(DyeColor.class);

        switch (color) {
            default:
            case BLACK:
                id = ItemIds.INK_SAC;
                break;
            case RED:
                id = ItemIds.RED_DYE;
                break;
            case GREEN:
                id = ItemIds.GREEN_DYE;
                break;
            case BROWN:
                id = ItemIds.COCOA_BEANS;
                break;
            case BLUE:
                id = ItemIds.LAPIS_LAZULI;
                break;
            case PURPLE:
                id = ItemIds.PURPLE_DYE;
                break;
            case CYAN:
                id = ItemIds.CYAN_DYE;
                break;
            case LIGHT_GRAY:
                id = ItemIds.LIGHT_GRAY_DYE;
                break;
            case GRAY:
                id = ItemIds.GRAY_DYE;
                break;
            case PINK:
                id = ItemIds.PINK_DYE;
                break;
            case LIME:
                id = ItemIds.LIME_DYE;
                break;
            case YELLOW:
                id = ItemIds.YELLOW_DYE;
                break;
            case LIGHT_BLUE:
                id = ItemIds.LIGHT_BLUE_DYE;
                break;
            case MAGENTA:
                id = ItemIds.MAGENTA_DYE;
                break;
            case ORANGE:
                id = ItemIds.ORANGE_DYE;
                break;
            case WHITE:
                id = ItemIds.BONE_MEAL;
                break;
        }

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);

        if (id == ItemIds.INK_SAC || id == ItemIds.BLACK_DYE) {
            builder.itemData(DyeColor.BLACK);
            return;
        }

        if (id == ItemIds.RED_DYE) {
            builder.itemData(DyeColor.RED);
            return;
        }

        if (id == ItemIds.GREEN_DYE) {
            builder.itemData(DyeColor.GREEN);
            return;
        }
        if (id == ItemIds.COCOA_BEANS || id == ItemIds.BROWN_DYE) {
            builder.itemData(DyeColor.BROWN);
            return;
        }
        if (id == ItemIds.LAPIS_LAZULI || id == ItemIds.BLUE_DYE) {
            builder.itemData(DyeColor.BLUE);
            return;
        }
        if (id == ItemIds.PURPLE_DYE) {
            builder.itemData(DyeColor.PURPLE);
            return;
        }
        if (id == ItemIds.CYAN_DYE) {
            builder.itemData(DyeColor.CYAN);
            return;
        }
        if (id == ItemIds.LIGHT_GRAY_DYE) {
            builder.itemData(DyeColor.LIGHT_GRAY);
            return;
        }
        if (id == ItemIds.GRAY_DYE) {
            builder.itemData(DyeColor.GRAY);
            return;
        }
        if (id == ItemIds.PINK_DYE) {
            builder.itemData(DyeColor.PINK);
            return;
        }
        if (id == ItemIds.LIME_DYE) {
            builder.itemData(DyeColor.LIME);
            return;
        }
        if (id == ItemIds.YELLOW_DYE) {
            builder.itemData(DyeColor.YELLOW);
            return;
        }
        if (id == ItemIds.LIGHT_BLUE_DYE) {
            builder.itemData(DyeColor.LIGHT_BLUE);
            return;
        }
        if (id == ItemIds.MAGENTA_DYE) {
            builder.itemData(DyeColor.MAGENTA);
            return;
        }
        if (id == ItemIds.ORANGE_DYE) {
            builder.itemData(DyeColor.ORANGE);
            return;
        }
        if (id == ItemIds.BONE_MEAL || id == ItemIds.WHITE_DYE) {
            builder.itemData(DyeColor.WHITE);
            return;
        }

        builder.itemData(DyeColor.BLACK);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
