package org.cloudburstmc.server.item.data.serializer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ArmorTrim;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

public class ArmorTrimSerializer implements ItemDataComponentSerializer<ArmorTrim> {

    private static final String TAG = "Trim";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, ArmorTrim trim) {
        tag.putCompound(TAG, NbtMap.builder()
                .putString("Pattern", trim.patternId())
                .putString("Material", trim.materialId())
                .build());
    }

    @Override
    public @Nullable ArmorTrim deserialize(Identifier id, NbtMap tag) {
        if (!tag.containsKey(TAG, NbtType.COMPOUND)) {
            return null;
        }

        NbtMap trim = tag.getCompound(TAG);
        String pattern = trim.getString("Pattern");
        String material = trim.getString("Material");
        return pattern.isBlank() || material.isBlank() ? null : new ArmorTrim(pattern, material);
    }
}
