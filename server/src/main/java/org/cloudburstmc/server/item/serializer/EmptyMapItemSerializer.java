package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

public class EmptyMapItemSerializer extends DefaultItemSerializer {

    private static final int LOCATOR_VARIANT = 2;

    @Override
    public int getAuxValue(ItemStack item) {
        return item.getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false) ? LOCATOR_VARIANT : 0;
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        builder.setData(ItemDataComponents.MAP_TRACKING_POSITION, meta == LOCATOR_VARIANT);
    }
}
