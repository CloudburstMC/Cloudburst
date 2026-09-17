package org.cloudburstmc.server.item.serializer;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

@Log4j2
public class BannerSerializer extends DefaultItemSerializer {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);

        int meta = item.get(ItemDataComponents.COLOR).getDyeData();
        itemTag.putShort("Damage", (short) meta);
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        builder.setData(ItemDataComponents.COLOR, DyeColor.getByWoolData(meta));

        super.deserialize(id, meta, builder, tag);
    }

}
