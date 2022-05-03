package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;

import java.util.Map;

public class BannerSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(DyeColor.class, DyeColor.WHITE);
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);

        int meta = item.get(ItemKeys.COLOR).getDyeData();
        itemTag.putShort("Damage", (short) meta);
    }

    @Override
    public void deserialize(Identifier id, ItemStackBuilder builder, NbtMap tag) {
        //TODO ???
//        nbtBuilder.putCompound("tag", tagBuilder.putInt("Base", meta).build());

        super.deserialize(id, builder, tag);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
