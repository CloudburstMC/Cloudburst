package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

public class OminousBottleItemSerializer extends DefaultItemSerializer {

    @Override
    public int getAuxValue(ItemStack item) {
        return item.getOrDefault(ItemDataComponents.OMINOUS_BOTTLE_AMPLIFIER, 0);
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        if (meta < 0 || meta > 4) {
            throw new IllegalArgumentException("Invalid ominous bottle amplifier: " + meta);
        }

        builder.setData(ItemDataComponents.OMINOUS_BOTTLE_AMPLIFIER, (int) meta);
    }
}
