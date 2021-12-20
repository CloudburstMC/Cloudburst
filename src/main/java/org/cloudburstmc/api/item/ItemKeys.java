package org.cloudburstmc.api.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.ListDataKey;
import org.cloudburstmc.api.data.SimpleDataKey;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.FireworkData;

public final class ItemKeys {

    public static final SimpleDataKey<BlockState> BLOCK_STATE = DataKey.simple(Identifier.fromString("block_state"), BlockState.class);

    public static final SimpleDataKey<Integer> DAMAGE = DataKey.simple(Identifier.fromString("item_damage"), Integer.class);

    public static final SimpleDataKey<DyeColor> COLOR = DataKey.simple(Identifier.fromString("dye_color"), DyeColor.class);

    public static final ListDataKey<Enchantment> ENCHANTMENTS = DataKey.list(Identifier.fromString("enchantments"), Enchantment.class);

    public static final SimpleDataKey<FireworkData> FIREWORK_DATA = DataKey.simple(Identifier.fromString("firework_data"), FireworkData.class);

    public static final SimpleDataKey<Bucket> BUCKET_DATA = DataKey.simple(Identifier.fromString("bucket_data"), Bucket.class);
}
