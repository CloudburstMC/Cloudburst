package org.cloudburstmc.server.item.data.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.Coal;
import org.cloudburstmc.api.util.Identifier;

@RequiredArgsConstructor
public class EnumSerializer<T extends Enum<T>> implements ItemDataSerializer<T> {

    public static final EnumSerializer<Coal> COAL = new EnumSerializer<>(Coal.class);

    private final Class<T> enumClass;

    @Override
    public void serialize(ItemStack item, NbtMapBuilder rootTag, NbtMapBuilder dataTag, T value) {
        rootTag.putShort("Damage", (short) value.ordinal());
    }

    @Override
    public T deserialize(Identifier id, NbtMap rootTag, NbtMap dataTag) {
        var constants = enumClass.getEnumConstants();
        return constants[rootTag.getShort("Damage") % constants.length];
    }
}
