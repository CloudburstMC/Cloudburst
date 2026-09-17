package org.cloudburstmc.server.item.serializer;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemDataComponentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class EnumDamageSerializer<T extends Enum<T>> extends DefaultItemSerializer {

    public static final EnumDamageSerializer<DyeColor> DYE_COLOR = new EnumDamageSerializer<>(ItemDataComponents.COLOR, DyeColor.class);

    private final ItemDataComponentType<T> dataType;

    private final T[] values;

    public EnumDamageSerializer(ItemDataComponentType<T> dataType, Class<T> enumClass) {
        this.dataType = dataType;
        this.values = enumClass.getEnumConstants();

        Preconditions.checkArgument(values.length > 0, "Enum must contain at least one constant");
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);

        T val = item.get(this.dataType);
        itemTag.putShort("Damage", (short) (val == null ? 0 : val.ordinal()));
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        builder.setData(this.dataType, values[meta % values.length]);
    }
}
