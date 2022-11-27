package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class EnumDamageSerializer<T extends Enum<T>> extends DefaultItemSerializer {

    public static final EnumDamageSerializer<DyeColor> DYE_COLOR = new EnumDamageSerializer<>(ItemKeys.COLOR, DyeColor.class);

    private final DataKey<T, T> dataKey;

    private final Class<T> enumClass;
    private final T[] values;

    public EnumDamageSerializer(DataKey<T, T> dataKey, Class<T> enumClass) {
        this.dataKey = dataKey;
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();

        Preconditions.checkArgument(values.length > 0, "Enum must contain at least one constant");
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);

        var val = item.get(this.dataKey);
        itemTag.putShort("Damage", (short) (val == null ? 0 : val.ordinal()));
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        builder.data(this.dataKey, values[meta % values.length]);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return Map.of(enumClass, values[0]);
    }
}
