package org.cloudburstmc.server.item.data.serializer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.VanillaItemDyes;

public class DyedColorSerializer implements ItemDataComponentSerializer<Integer> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, Integer value) {
        if ((value & 0xFF000000) != 0) {
            throw new IllegalArgumentException("Color must be a 24-bit RGB value");
        }
        tag.putInt("customColor", 0xFF000000 | value);
    }

    @Override
    public @Nullable Integer deserialize(Identifier id, NbtMap tag) {
        boolean dyeable = VanillaItemDyes.isDyeable(id) || !"minecraft".equals(id.getNamespace());
        return dyeable && tag.containsKey("customColor", NbtType.INT) ? tag.getInt("customColor") & 0xFFFFFF : null;
    }
}
