package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;

public class BannerSerializer extends DefaultItemSerializer {

    @Override
    public int getAuxValue(ItemStack item) {
        BannerData banner = item.get(ItemDataComponents.BANNER_DATA);
        return (banner == null ? item.getOrDefault(ItemDataComponents.COLOR, DyeColor.BLACK) : banner.getBase()).getDyeData();
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        builder.setData(ItemDataComponents.COLOR, DyeColor.getByDyeData(meta));

        super.deserialize(id, meta, builder, tag);
    }

}
