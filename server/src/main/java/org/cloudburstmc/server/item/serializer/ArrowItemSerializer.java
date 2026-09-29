package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.network.NetworkUtils;

public class ArrowItemSerializer extends DefaultItemSerializer {

    @Override
    public int getAuxValue(ItemStack item) {
        PotionType potion = item.get(ItemDataComponents.POTION_TYPE);
        return potion == null ? 0 : NetworkUtils.potionToNetwork(potion) + 1;
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        if (meta != 0) {
            builder.setData(ItemDataComponents.POTION_TYPE, NetworkUtils.potionFromNetwork((short) (meta - 1)));
        }
    }
}
