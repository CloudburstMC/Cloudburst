package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.val;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

public class EnumDamageSerializer extends DefaultItemSerializer {

    public static final EnumDamageSerializer DYE_COLOR = new EnumDamageSerializer(DyeColor.class);

    private final Class<? extends Enum<?>> enumClass;
    private final Enum<?>[] values;

    public EnumDamageSerializer(Class<? extends Enum<?>> enumClass) {
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        val val = item.getMetadata(enumClass);
        itemTag.putShort("Damage", (short) (val == null ? 0 : val.ordinal()));
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);
        builder.itemData(values[meta % values.length]);
    }
}
