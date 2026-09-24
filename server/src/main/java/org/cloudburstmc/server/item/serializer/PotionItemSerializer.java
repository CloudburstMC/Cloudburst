package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.network.NetworkUtils;

public class PotionItemSerializer extends DefaultItemSerializer {

    @Override
    public int getAuxValue(ItemStack item) {
        return NetworkUtils.potionToNetwork(item.getOrDefault(ItemDataComponents.POTION_TYPE, PotionTypes.WATER));
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        builder.setData(ItemDataComponents.POTION_TYPE, NetworkUtils.potionFromNetwork(meta));
    }
}
