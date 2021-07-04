package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;

import java.util.Map;

public class EnumDamageSerializer extends DefaultItemSerializer {

    public static final EnumDamageSerializer DYE_COLOR = new EnumDamageSerializer(DyeColor.class);

    private final Class<? extends Enum<?>> enumClass;
    private final Enum<?>[] values;

    public EnumDamageSerializer(Class<? extends Enum<?>> enumClass) {
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();

        Preconditions.checkArgument(values.length > 0, "Enum must contain at least one constant");
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        var val = item.getMetadata(enumClass);
        itemTag.putShort("Damage", (short) (val == null ? 0 : val.ordinal()));
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);
        builder.itemData(values[meta % values.length]);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return Map.of(enumClass, values[0]);
    }
}
