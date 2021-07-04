package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;

import java.util.Map;

public class BannerSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(DyeColor.class, DyeColor.WHITE);
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);

        int meta = item.getMetadata(DyeColor.class, DyeColor.WHITE).getDyeData();
        itemTag.putShort("Damage", (short) meta);
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        var nbtBuilder = tag.toBuilder();
        NbtMapBuilder tagBuilder;
        if (tag.containsKey("tag", NbtType.COMPOUND)) {
            tagBuilder = tag.getCompound("tag").toBuilder();
        } else {
            tagBuilder = NbtMap.builder();
        }

        nbtBuilder.putCompound("tag", tagBuilder.putInt("Base", meta).build());

        super.deserialize(id, meta, amount, builder, nbtBuilder.build());
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
